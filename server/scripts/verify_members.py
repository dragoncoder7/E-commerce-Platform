"""Real Gateway/MySQL/Redis member checks. Local captured email only; never sends real mail."""
import hashlib
import json
from pathlib import Path
import time
import pymysql
from verify_catalog import request


def main():
    prefix = 'member_' + str(time.time_ns())
    password = 'MemberTest123!'
    env = dict(line.split('=', 1) for line in Path('.runtime/verify.env').read_text().splitlines())
    db = pymysql.connect(host='127.0.0.1', port=13306, user='root', password=env['DB_PASSWORD'], database='ecommerce_system', autocommit=True)
    assert request('store/auth/capabilities')['data']['mailMode'] == 'capture'
    assert request('store/auth/me')['code'] == 401
    registered = request('store/auth/register', {'type': 'account', 'account': prefix, 'password': password, 'status': 0, 'role': 'admin'})
    assert registered['code'] == 200, registered
    assert 'passwordHash' not in registered['data']
    assert request('store/auth/register', {'type': 'account', 'account': prefix, 'password': password})['code'] == 409
    assert request('store/auth/login', {'account': prefix, 'password': 'incorrect'})['code'] == 401
    login = request('store/auth/login', {'account': prefix.upper(), 'password': password})
    assert login['code'] == 200, login
    token = login['data']['token']
    second = request('store/auth/login', {'account': prefix, 'password': password})['data']['token']
    assert token != second, 'Every login must have its own session ID'
    assert request('store/auth/me', token=token)['data']['id'] == registered['data']['id']
    assert request('admin/product/list', token=token)['code'] == 401
    erp = request('admin/auth/login', {'username': 'catalog_admin', 'password': 'CatalogTest123!'})['data']['token']
    assert request('store/auth/me', token=erp)['code'] == 401
    assert request('store/auth/me', {'nickname': '测试会员'}, method='PUT', token=token)['data']['nickname'] == '测试会员'
    assert request('store/auth/logout', {}, token=token)['code'] == 200
    assert request('store/auth/me', token=token)['code'] == 401
    assert request('store/auth/me', token=second)['code'] == 200
    assert request('store/auth/password', {'currentPassword': password, 'newPassword': 'Changed123!'}, method='PUT', token=second)['code'] == 200
    assert request('store/auth/me', token=second)['code'] == 401
    assert request('store/auth/login', {'account': prefix, 'password': password})['code'] == 401
    current = request('store/auth/login', {'account': prefix, 'password': 'Changed123!'})['data']['token']
    with db.cursor() as cursor:
        cursor.execute('UPDATE member_user SET status=0 WHERE id=%s', (registered['data']['id'],))
    assert request('store/auth/me', token=current)['code'] == 401
    with db.cursor() as cursor:
        cursor.execute('UPDATE member_user SET status=1 WHERE id=%s', (registered['data']['id'],))
    phone = '139' + str(time.time_ns())[-8:]
    assert request('store/auth/register', {'type': 'phone', 'account': phone, 'password': password})['data']['phoneVerified'] is False
    assert request('store/auth/login', {'account': phone, 'password': password})['code'] == 200
    email = prefix + '@example.com'
    assert request('store/auth/email-code', {'email': email})['code'] == 200
    assert request('store/auth/email-code', {'email': email})['code'] == 429
    captured = json.loads(Path('.runtime/mail/' + hashlib.sha256(email.encode()).hexdigest() + '.json').read_text())
    code = captured['code']
    assert request('store/auth/register', {'type': 'email', 'account': email, 'password': password, 'code': '999999' if code != '999999' else '888888'})['code'] == 400
    assert request('store/auth/register', {'type': 'email', 'account': email.upper(), 'password': password, 'code': code})['code'] == 200
    assert request('store/auth/register', {'type': 'email', 'account': email, 'password': password, 'code': code})['code'] == 400
    assert request('store/auth/login', {'account': email.upper(), 'password': password})['code'] == 200
    locked_email = 'locked_' + email
    assert request('store/auth/email-code', {'email': locked_email})['code'] == 200
    locked_code = json.loads(Path('.runtime/mail/' + hashlib.sha256(locked_email.encode()).hexdigest() + '.json').read_text())['code']
    for _ in range(5):
        assert request('store/auth/register', {'type': 'email', 'account': locked_email, 'password': password, 'code': '999999' if locked_code != '999999' else '888888'})['code'] == 400
    assert request('store/auth/register', {'type': 'email', 'account': locked_email, 'password': password, 'code': locked_code})['code'] == 400
    with db.cursor() as cursor:
        cursor.execute('SELECT password_hash,status FROM member_user WHERE username=%s', (prefix,))
        row = cursor.fetchone()
        assert row[0].startswith('$2') and row[1] == 1
        cursor.execute('SELECT COUNT(*) FROM sys_user WHERE username=%s', (prefix,))
        assert cursor.fetchone()[0] == 0
    Path('.runtime/member-result.json').write_text(json.dumps({'account': prefix, 'result': 'passed', 'checks': 29}, ensure_ascii=False))
    print('PASS: 29 member checks (account/phone/email, uniqueness, password, logout, disable, session revocation, OTP, cross-service tokens)')


if __name__ == '__main__':
    main()
