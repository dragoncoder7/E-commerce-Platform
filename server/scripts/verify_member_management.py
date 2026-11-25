"""ERP read/write permissions and member session revocation on the local verification DB."""
import json
from pathlib import Path
import time
import bcrypt
import pymysql
from verify_catalog import request


def main():
    env = dict(line.split('=', 1) for line in Path('.runtime/verify.env').read_text().splitlines())
    db = pymysql.connect(host='127.0.0.1', port=13306, user='root', password=env['DB_PASSWORD'], database='ecommerce_system', autocommit=True)
    stamp = str(time.time_ns())
    member_name = 'erpcheck_' + stamp
    registered = request('store/auth/register', {'type': 'account', 'account': member_name, 'password': 'MemberTest123!'})
    assert registered['code'] == 200, registered
    member_id = registered['data']['id']
    member_token = request('store/auth/login', {'account': member_name, 'password': 'MemberTest123!'})['data']['token']
    admin_token = request('admin/auth/login', {'username': 'catalog_admin', 'password': 'CatalogTest123!'})['data']['token']
    assert request('admin/members')['code'] == 401
    assert request('admin/members', token=member_token)['code'] == 401
    assert request('admin/members?pageSize=101', token=admin_token)['code'] == 400
    listed = request('admin/members?keyword=' + member_name, token=admin_token)
    assert listed['code'] == 200 and listed['data']['total'] == 1, listed
    assert 'passwordHash' not in listed['data']['records'][0] and 'sessionVersion' not in listed['data']['records'][0]
    for invalid in [-1, 2]:
        assert request(f'admin/members/{member_id}/status', {'status': invalid}, method='PUT', token=admin_token)['code'] == 400
    with db.cursor() as cursor:
        # Reader additionally has a disabled write role: disabled roles must not grant permissions.
        cursor.execute("INSERT INTO sys_role(name,code,status) VALUES('会员只读验收',%s,1)", ('member_reader_' + stamp,))
        read_role = cursor.lastrowid
        cursor.execute("INSERT INTO sys_role(name,code,status) VALUES('停用写入角色验收',%s,0)", ('member_disabled_' + stamp,))
        disabled_role = cursor.lastrowid
        for role_id, permission in [(read_role, 'members:read'), (disabled_role, 'members:write')]:
            cursor.execute("INSERT INTO sys_menu(name,title,type,path,permission) VALUES(%s,'会员验收权限','button','',%s)", (permission + stamp, permission))
            cursor.execute('INSERT INTO sys_role_menu(role_id,menu_id) VALUES(%s,%s)', (role_id, cursor.lastrowid))
        reader = 'reader_' + stamp
        cursor.execute("INSERT INTO sys_user(username,password,nickname) VALUES(%s,%s,'会员只读验收')", (reader, bcrypt.hashpw(b'ReaderTest123!', bcrypt.gensalt()).decode()))
        reader_id = cursor.lastrowid
        cursor.executemany('INSERT INTO sys_user_role(user_id,role_id) VALUES(%s,%s)', [(reader_id, read_role), (reader_id, disabled_role)])
    reader_token = request('admin/auth/login', {'username': reader, 'password': 'ReaderTest123!'})['data']['token']
    assert request('admin/members', token=reader_token)['code'] == 200
    assert request(f'admin/members/{member_id}/status', {'status': 0}, method='PUT', token=reader_token)['code'] == 403
    assert request('admin/product/list', token=reader_token)['code'] == 403
    assert request(f'admin/members/{member_id}/status', {'status': 0}, method='PUT', token=admin_token)['code'] == 200
    assert request('store/auth/me', token=member_token)['code'] == 401
    assert request('store/addresses', token=member_token)['code'] == 401
    assert request('store/auth/login', {'account': member_name, 'password': 'MemberTest123!'})['code'] == 401
    assert request(f'admin/members/{member_id}/status', {'status': 1}, method='PUT', token=admin_token)['code'] == 200
    assert request('store/auth/me', token=member_token)['code'] == 401, 'Re-enabling must not resurrect old sessions'
    assert request('store/auth/login', {'account': member_name, 'password': 'MemberTest123!'})['code'] == 200
    Path('.runtime/member-management-result.json').write_text(json.dumps({'result': 'passed', 'reader': reader, 'member': member_name, 'memberId': member_id, 'disabledRoleRejected': True, 'oldSessionsRevokedAfterRestore': True}))
    print('PASS: ERP member search/status, anonymous/member denial, read-only role, disabled role cannot grant writes, cross-service revocation and restore without token revival')
    db.close()


if __name__ == '__main__':
    main()
