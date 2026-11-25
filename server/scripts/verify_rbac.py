"""Real ERP staff/role/menu lifecycle, security boundaries, and session revocation."""
import json
from pathlib import Path
import time
from verify_catalog import request


def main():
    stamp = str(time.time_ns())
    token = request('admin/auth/login', {'username': 'catalog_admin', 'password': 'CatalogTest123!'})['data']['token']
    def call(path, data=None, method=None, credential=token):
        return request('admin/' + path, data, method=method, token=credential)
    def flatten(nodes):
        return [item for node in nodes for item in [node, *flatten(node.get('children') or [])]]
    assert call('staff')['code'] == 200
    assert call('roles')['code'] == 200
    menu_rows = flatten(call('menu/list')['data'])
    product_read = next(row['id'] for row in menu_rows if row['permission'] == 'product:read')
    member_read = next(row['id'] for row in menu_rows if row['permission'] == 'members:read')
    role = {'name': '验收只读角色', 'code': 'audit_' + stamp, 'status': 1, 'sort': 0, 'remark': '可删除验收角色', 'menuIds': [product_read, member_read]}
    saved = call('roles', role)
    assert saved['code'] == 200, saved
    role_id = saved['data']['id']
    assert call('roles', role)['code'] == 409
    staff = {'username': 'staff_' + stamp, 'nickname': '验收人员', 'password': 'StaffTest123!', 'status': 1, 'roleIds': [role_id]}
    saved = call('staff', staff)
    assert saved['code'] == 200, saved
    staff_id = saved['data']['id']
    assert 'password' not in saved['data'] and 'sessionVersion' not in saved['data']
    staff_token = call('auth/login', {'username': staff['username'], 'password': staff['password']})['data']['token']
    assert call('members', credential=staff_token)['code'] == 200
    assert call('product/list', credential=staff_token)['code'] == 200
    assert call('staff', credential=staff_token)['code'] == 403
    assert call('roles', role, credential=staff_token)['code'] == 403
    assert call('menu', {}, credential=staff_token)['code'] == 403
    assert call(f'roles/{role_id}', method='DELETE')['code'] == 409
    # Revocation at every request: role removal immediately removes resource access.
    role['menuIds'] = [member_read]
    assert call(f'roles/{role_id}', role, 'PUT')['code'] == 200
    assert call('product/list', credential=staff_token)['code'] == 403
    assert call('members', credential=staff_token)['code'] == 200
    role['status'] = 0
    assert call(f'roles/{role_id}', role, 'PUT')['code'] == 200
    assert call('members', credential=staff_token)['code'] == 403
    role['status'] = 1
    assert call(f'roles/{role_id}', role, 'PUT')['code'] == 200
    # Password reset and user disable/re-enable do not resurrect sessions.
    staff['password'] = 'NewStaff123!'
    assert call(f'staff/{staff_id}', staff, 'PUT')['code'] == 200
    assert call('members', credential=staff_token)['code'] == 401
    assert call('auth/login', {'username': staff['username'], 'password': 'StaffTest123!'})['code'] != 200
    fresh = call('auth/login', {'username': staff['username'], 'password': staff['password']})['data']['token']
    staff['password'] = ''
    staff['status'] = 0
    assert call(f'staff/{staff_id}', staff, 'PUT')['code'] == 200
    assert call('members', credential=fresh)['code'] in [401, 403]
    staff['status'] = 1
    assert call(f'staff/{staff_id}', staff, 'PUT')['code'] == 200
    assert call('members', credential=fresh)['code'] == 401
    # Reserved administrator safety and self lockout.
    me = call('auth/userinfo')['data']
    admin = next(row for row in call('roles')['data'] if row['code'] == 'admin')
    assert call(f"roles/{admin['id']}", {**admin, 'status': 0}, 'PUT')['code'] == 409
    assert call(f"roles/{admin['id']}", method='DELETE')['code'] == 409
    assert call(f"staff/{me['id']}", method='DELETE')['code'] == 409
    assert call(f"staff/{me['id']}", {'username': me['username'], 'nickname': me['nickname'], 'status': 0, 'roleIds': [admin['id']], 'password': ''}, 'PUT')['code'] == 409
    # Menu tree CRUD, cycle prevention and role reference deletion protection.
    parent = {'parentId': 0, 'name': 'AuditMenu' + stamp, 'title': '验收目录', 'type': 'catalog', 'path': '/audit-' + stamp, 'component': '', 'icon': '', 'permission': '', 'sort': 90, 'visible': True, 'keepAlive': False, 'status': 1}
    created_parent = call('menu', parent)
    assert created_parent['code'] == 200, created_parent
    parent_id = created_parent['data']['id']
    child = {**parent, 'parentId': parent_id, 'name': 'AuditChild' + stamp, 'title': '验收子菜单', 'type': 'menu', 'path': '/audit-child-' + stamp, 'component': 'member/index', 'permission': 'members:read'}
    created_child = call('menu', child)
    assert created_child['code'] == 200, created_child
    child_id = created_child['data']['id']
    assert call(f'menu/{parent_id}', {**parent, 'parentId': child_id}, 'PUT')['code'] == 400
    assert call(f'menu/{parent_id}', method='DELETE')['code'] == 409
    assert call('menu', {**child, 'name': 'Unsafe' + stamp, 'path': '//example.com'}, 'POST')['code'] == 400
    button = {**parent, 'parentId': child_id, 'name': 'AuditButton' + stamp, 'title': '验收写按钮', 'type': 'button', 'path': '', 'permission': 'members:write'}
    button_saved = call('menu', button)
    assert button_saved['code'] == 200, button_saved
    button_id = button_saved['data']['id']
    role['menuIds'] = [parent_id, child_id, button_id]
    assert call(f'roles/{role_id}', role, 'PUT')['code'] == 200
    assert call(f'menu/{button_id}', method='DELETE')['code'] == 409
    assert call(f'staff/{staff_id}', method='DELETE')['code'] == 200
    assert call(f'roles/{role_id}', method='DELETE')['code'] == 200
    for mid in [button_id, child_id, parent_id]:
        assert call(f'menu/{mid}', method='DELETE')['code'] == 200
    Path('.runtime/rbac-result.json').write_text(json.dumps({'result': 'passed', 'staffRoleMenuCrud': True, 'permissionRevocation': True, 'passwordAndDisableRevocation': True, 'adminProtection': True, 'menuCycleAndReferences': True}))
    print('PASS: staff/role/menu CRUD, password reset, session revocation, live role changes, disabled roles, admin protection, menu cycle and reference checks')


if __name__ == '__main__':
    main()
