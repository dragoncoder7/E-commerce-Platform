"""Verify fresh Flyway initialization and checked legacy baseline upgrade in separate MySQL databases.

Creates uniquely named verification databases; never drops/reinitializes an existing database.
Run from server using .runtime/venv/Scripts/python scripts/verify_migrations.py.
"""
import json
import os
from pathlib import Path
import re
import subprocess
import time
from decimal import Decimal
import pymysql

ROOT = Path(__file__).resolve().parents[1]


def ddl_statements(source):
    return re.findall(r'CREATE TABLE\s+`?\w+`?\s*\(.*?\)\s*ENGINE=.*?;', source, flags=re.DOTALL | re.IGNORECASE)


def main():
    env = dict(line.split('=', 1) for line in (ROOT / '.runtime/verify.env').read_text().splitlines())
    info = json.loads(subprocess.check_output(['docker', 'inspect', 'ecommerce-b2c-mysql-1']))[0]
    assert info['Config']['Labels'].get('com.docker.compose.project') == 'ecommerce-b2c'
    db = pymysql.connect(host='127.0.0.1', port=13306, user='root', password=env['DB_PASSWORD'], autocommit=True, charset='utf8mb4')
    suffix = str(time.time_ns())
    fresh = 'b2c_fresh_' + suffix
    legacy = 'b2c_legacy_' + suffix
    child_env = os.environ.copy()
    child_env['FLYWAY_PASSWORD'] = env['DB_PASSWORD']
    def migrate(database, action='migrate', target=None):
        assert re.fullmatch(r'b2c_(fresh|legacy)_[0-9]+', database)
        command = ['docker', 'run', '--rm', '--network', 'ecommerce-b2c_default', '-e', 'FLYWAY_PASSWORD',
                   '-v', str(ROOT / 'sql/migrations') + ':/flyway/sql:ro', 'flyway/flyway:10.20.1',
                   '-url=jdbc:mysql://mysql:3306/' + database + '?allowPublicKeyRetrieval=true&useSSL=false', '-user=root']
        if target is not None:
            command += ['-target=' + str(target)]
        if action == 'baseline':
            command += ['-baselineVersion=1']
        result = subprocess.run(command + [action], env=child_env, capture_output=True, text=True)
        if result.returncode:
            print(result.stdout)
            print(result.stderr)
            raise RuntimeError('Flyway ' + action + ' failed')
        return result.stdout
    with db.cursor() as cursor:
        for database in [fresh, legacy]:
            cursor.execute('CREATE DATABASE `' + database + '` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci')
    print('Created fresh and legacy verification databases', flush=True)
    migrate(fresh, target=1)
    # Rebuild the prior schema from the repository legacy schema definitions only, never execute their USE/seed commands.
    legacy_ddl = ddl_statements((ROOT / 'sql/init.sql').read_text(encoding='utf-8'))
    legacy_ddl = [statement for statement in legacy_ddl if re.match(r'CREATE TABLE `sys_', statement)]
    legacy_ddl += ddl_statements((ROOT / 'sql/product_module.sql').read_text(encoding='utf-8'))
    assert len(legacy_ddl) == 10, len(legacy_ddl)
    db.select_db(legacy)
    with db.cursor() as cursor:
        for statement in legacy_ddl:
            cursor.execute(statement)
        baseline_tables = re.findall(r'CREATE TABLE `([^`]+)`', (ROOT / 'sql/migrations/V001__baseline.sql').read_text(encoding='utf-8'))
        # Fail closed on missing columns, type, nullability or key differences before permitting baseline.
        def columns(database, table):
            cursor.execute('SELECT column_name,column_type,is_nullable,column_key FROM information_schema.columns WHERE table_schema=%s AND table_name=%s ORDER BY ordinal_position', (database, table))
            return tuple(cursor.fetchall())
        for table in baseline_tables:
            assert columns(legacy, table) == columns(fresh, table), 'Legacy baseline mismatch: ' + table
        cursor.execute("INSERT INTO sys_user(username,password,nickname) VALUES('preserved_user','fixture-only-hash','原后台账号')")
        cursor.execute("INSERT INTO product_category(category_name,category_code) VALUES('旧分类','LEGACY')")
        category_id = cursor.lastrowid
        cursor.execute("INSERT INTO product(product_code,product_name,category_id,sale_price,stock,status) VALUES('LEGACY-CUP','旧商品',%s,88.25,17,1)", (category_id,))
        product_id = cursor.lastrowid
        cursor.execute('SELECT id,product_code,product_name,category_id,sale_price,stock,status FROM product WHERE id=%s', (product_id,))
        before = cursor.fetchone()
        cursor.execute("INSERT INTO product(product_code,product_name,category_id,sale_price,stock,status,deleted) VALUES('LEGACY-DELETED','已删除商品',%s,1,2,0,1)", (category_id,))
    migrate(fresh)
    migrate(fresh)
    migrate(fresh, 'validate')
    migrate(legacy, 'baseline')
    migrate(legacy)
    migrate(legacy)
    migrate(legacy, 'validate')
    with db.cursor() as cursor:
        cursor.execute('SELECT id,product_code,product_name,category_id,sale_price,stock,status FROM product WHERE id=%s', (product_id,))
        assert cursor.fetchone() == before
        cursor.execute('SELECT sku_code,specification,price,stock,status FROM product_sku WHERE product_id=%s', (product_id,))
        assert cursor.fetchall() == (('LEGACY-CUP-DEFAULT', '默认规格', Decimal('88.25'), 17, 1),)
        cursor.execute('SELECT COUNT(*) FROM product_sku')
        assert cursor.fetchone()[0] == 1
        cursor.execute("SELECT nickname FROM sys_user WHERE username='preserved_user'")
        assert cursor.fetchone()[0] == '原后台账号'
        cursor.execute('SELECT COUNT(*) FROM member_user')
        assert cursor.fetchone()[0] == 0, 'ERP users must not be copied into storefront membership'
        cursor.execute('SELECT version,success FROM flyway_schema_history ORDER BY installed_rank')
        versions = cursor.fetchall()
        assert all(row[1] == 1 for row in versions)
    (ROOT / '.runtime/migration-result.json').write_text(json.dumps({'fresh': fresh, 'legacy': legacy, 'result': 'passed', 'legacySchemaColumnsVerified': len(baseline_tables), 'defaultSkuPreserved': True, 'repeatMigration': 'no-op', 'versions': versions}, ensure_ascii=False), encoding='utf-8')
    print('PASS: fresh initialize/repeat/validate, legacy schema preflight/baseline/upgrade, old product and ERP account preserved, exactly one default SKU; no databases deleted')
    db.close()


if __name__ == '__main__':
    main()
