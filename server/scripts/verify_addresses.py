"""Address ownership, default uniqueness and concurrency against the local MySQL stack."""
import concurrent.futures
import json
from pathlib import Path
import time
import pymysql
from verify_catalog import request


def main():
    env = dict(line.split('=', 1) for line in Path('.runtime/verify.env').read_text().splitlines())
    db = pymysql.connect(host='127.0.0.1', port=13306, user='root', password=env['DB_PASSWORD'], database='ecommerce_system', autocommit=True)
    def member(suffix):
        name = 'address_' + str(time.time_ns()) + suffix
        registered = request('store/auth/register', {'type': 'account', 'account': name, 'password': 'AddressTest123!'})
        assert registered['code'] == 200, registered
        login = request('store/auth/login', {'account': name, 'password': 'AddressTest123!'})
        return registered['data']['id'], login['data']['token']
    first_member, first_token = member('a')
    second_member, second_token = member('b')
    template = {'recipient': '测试收货人', 'phone': '13800138000', 'province': '浙江省', 'city': '杭州市', 'district': '西湖区', 'detail': '测试路1号', 'defaultAddress': False}
    assert request('store/addresses')['code'] == 401
    first = request('store/addresses', {**template, 'memberId': second_member}, token=first_token)
    assert first['code'] == 200 and first['data']['defaultAddress'], first
    aid = first['data']['id']
    assert 'memberId' not in first['data']
    assert request('store/addresses', token=second_token)['data'] == []
    for method, suffix, body in [('GET', '', None), ('PUT', '', template), ('DELETE', '', None), ('PUT', '/default', None)]:
        result = request(f'store/addresses/{aid}{suffix}', body, method=method, token=second_token)
        assert result['code'] == 404, result
    assert request('store/addresses', {**template, 'recipient': ' '}, token=first_token)['code'] == 400
    second = request('store/addresses', {**template, 'recipient': '第二地址'}, token=first_token)['data']
    assert not second['defaultAddress']
    assert request(f"store/addresses/{second['id']}/default", method='PUT', token=first_token)['code'] == 200
    rows = request('store/addresses', token=first_token)['data']
    assert len(rows) == 2 and sum(row['defaultAddress'] for row in rows) == 1 and rows[0]['id'] == second['id']
    updated = request(f'store/addresses/{aid}', {**template, 'detail': '新地址2号'}, method='PUT', token=first_token)
    assert updated['code'] == 200 and updated['data']['detail'] == '新地址2号'
    # Multiple concurrent default switches serialize on the member row; no duplicate default survives.
    def change(index):
        target = aid if index % 2 == 0 else second['id']
        return request(f'store/addresses/{target}/default', method='PUT', token=first_token)['code']
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        results = list(pool.map(change, range(16)))
    assert results == [200] * 16, results
    with db.cursor() as cursor:
        cursor.execute('SELECT COUNT(*) FROM member_address WHERE member_id=%s AND is_default=1', (first_member,))
        assert cursor.fetchone()[0] == 1
        cursor.execute('SELECT member_id FROM member_address WHERE id=%s', (aid,))
        assert cursor.fetchone()[0] == first_member
        try:
            cursor.execute('INSERT INTO member_address(member_id,recipient,phone,province,city,district,detail,is_default) VALUES(%s,%s,%s,%s,%s,%s,%s,1)', (first_member, '违规默认', '13800138000', '省', '市', '区', '路'))
            raise AssertionError('Database default-address uniqueness must reject duplicates')
        except pymysql.IntegrityError:
            pass
    rows = request('store/addresses', token=first_token)['data']
    assert request(f"store/addresses/{rows[0]['id']}", method='DELETE', token=first_token)['code'] == 200
    remaining = request('store/addresses', token=first_token)['data']
    assert len(remaining) == 1 and remaining[0]['defaultAddress']
    # Capacity check remains valid under concurrent creates.
    def create(index):
        return request('store/addresses', {**template, 'detail': f'并发地址{index}号'}, token=first_token)['code']
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        results = list(pool.map(create, range(25)))
    assert results.count(200) == 19 and results.count(409) == 6, results
    rows = request('store/addresses', token=first_token)['data']
    assert len(rows) == 20 and sum(row['defaultAddress'] for row in rows) == 1
    for row in rows:
        assert request(f"store/addresses/{row['id']}", method='DELETE', token=first_token)['code'] == 200
    assert request('store/addresses', token=first_token)['data'] == []
    Path('.runtime/address-result.json').write_text(json.dumps({'result': 'passed', 'concurrentDefaultSwitches': 16, 'concurrentCreates': 25, 'created': 19, 'capacityRejected': 6, 'ownershipAndDbConstraint': 'passed'}))
    print('PASS: address CRUD, four cross-member denials, forged owner ignored, default uniqueness, delete fallback, 16 concurrent switches and 25 concurrent creates')
    db.close()


if __name__ == '__main__':
    main()
