"""Run against the isolated compose.b2c.yml verification stack only.

Install bcrypt and pymysql in a local venv, then run from server/.
Credentials are read from ignored .runtime/verify.env; no real email is sent.
"""
import concurrent.futures
import base64
import uuid
import json
from pathlib import Path
import urllib.request
import urllib.parse

import bcrypt
import pymysql


def request(path, data=None, method=None, token=None):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    req = urllib.request.Request("http://localhost:18080/api/" + urllib.parse.quote(path, safe="/?=&"),
                                 data=json.dumps(data).encode() if data is not None else None,
                                 method=method, headers=headers)
    with urllib.request.urlopen(req, timeout=20) as response:
        return json.load(response)


def main():
    env = dict(line.split("=", 1) for line in Path(".runtime/verify.env").read_text().splitlines())
    db = pymysql.connect(host="127.0.0.1", port=13306, user="root", password=env["DB_PASSWORD"],
                         database="ecommerce_system", autocommit=True, charset="utf8mb4")
    with db.cursor() as cursor:
        password = bcrypt.hashpw(b"CatalogTest123!", bcrypt.gensalt()).decode()
        cursor.execute("INSERT INTO sys_role(name,code) VALUES('验证管理员','admin') ON DUPLICATE KEY UPDATE name=VALUES(name)")
        cursor.execute("INSERT INTO sys_user(username,password,nickname) VALUES('catalog_admin',%s,'验证管理员') ON DUPLICATE KEY UPDATE password=VALUES(password)", (password,))
        cursor.execute("INSERT IGNORE INTO sys_user_role(user_id,role_id) SELECT u.id,r.id FROM sys_user u,sys_role r WHERE u.username='catalog_admin' AND r.code='admin'")
    login = request("admin/auth/login", {"username": "catalog_admin", "password": "CatalogTest123!"})
    assert login["code"] == 200, login
    token = login["data"]["token"]
    assert request("admin/product/list")["code"] == 401
    assert request("admin/auth/register", {}, token=token)["code"] == 403
    categories = request("admin/product/category/all", token=token)["data"]
    category = next((c for c in categories if c["categoryCode"] == "VERIFY"), None)
    if not category:
        category = request("admin/product/category", {"categoryName": "日用好物", "categoryCode": "VERIFY", "parentId": 0, "status": 1, "sortOrder": 0}, token=token)["data"]
    import time
    code = "VERIFY_" + str(time.time_ns())
    product = request("admin/product", {"productCode": code, "productName": "素色陶瓷马克杯", "categoryId": category["id"], "salePrice": 49.90,
                      "stock": 10, "status": 1, "isRecommend": 1, "subTitle": "温润釉面，让每一杯都有温度", "description": "日常饮水与咖啡时光的实用之选。\n容量 350ml，手工质感釉面。"}, token=token)
    assert product["code"] == 200, product
    pid = product["data"]["id"]
    sku = request(f"admin/product/{pid}/skus", token=token)["data"][0]
    sid = sku["id"]
    assert sku["stock"] == 10
    def deduct(_):
        return request(f"admin/product/{pid}/skus/{sid}/stock", {"delta": -1, "reason": "并发库存验证"}, token=token)["code"]
    with concurrent.futures.ThreadPoolExecutor(max_workers=8) as pool:
        results = list(pool.map(deduct, range(15)))
    assert results.count(200) == 10 and results.count(409) == 5, results
    with db.cursor() as cursor:
        cursor.execute("SELECT stock FROM product_sku WHERE id=%s", (sid,))
        assert cursor.fetchone()[0] == 0
        cursor.execute("SELECT COUNT(*) FROM inventory_log WHERE sku_id=%s", (sid,))
        assert cursor.fetchone()[0] == 11
    request(f"admin/product/{pid}/skus/{sid}/stock", {"delta": 20, "reason": "恢复演示库存"}, token=token)
    second = request(f"admin/product/{pid}/skus", {"skuCode": code + "-BLUE", "specification": "雾蓝色 / 350ml", "price": 59.90, "status": 1}, method="PUT", token=token)
    assert second["code"] == 200, second
    request(f"admin/product/{pid}/skus/{second['data']['id']}/stock", {"delta": 12, "reason": "首次入库"}, token=token)
    catalog = request(f"store/catalog/products/{pid}")
    assert catalog["code"] == 200 and len(catalog["data"]["skus"]) == 2, catalog
    assert catalog["data"]["stock"] == 32
    assert "createBy" not in catalog["data"]
    assert request("store/catalog/products?pageSize=101")["code"] == 400
    assert request("store/catalog/products?sort=invalid")["code"] == 400
    assert request("store/catalog/products?keyword=素色")["code"] == 200
    # Product response contract and brand lifecycle, including duplicate conflicts.
    assert "deleted" not in request(f"admin/product/{pid}", token=token)["data"]
    brand_request = {"brandCode": code + "-B", "brandName": "验收品牌", "status": 1, "sortOrder": 0}
    brand = request("admin/product/brand", brand_request, token=token)
    assert brand["code"] == 200, brand
    assert request("admin/product/brand", brand_request, token=token)["code"] == 409
    product_edit = {"id": pid, "productCode": code, "productName": "素色陶瓷马克杯", "categoryId": category["id"],
                    "brandId": brand["data"]["id"], "salePrice": 999, "stock": 999, "status": 1, "isRecommend": 1}
    edited = request("admin/product", product_edit, method="PUT", token=token)
    assert edited["code"] == 200 and edited["data"]["stock"] == 32 and float(edited["data"]["salePrice"]) == 49.9, edited
    assert request(f"admin/product/brand/{brand['data']['id']}", method="DELETE", token=token)["code"] == 409
    assert request(f"admin/product/category/{category['id']}", method="DELETE", token=token)["code"] == 400
    assert request("store/catalog/products?minPrice=60&maxPrice=30")["code"] == 400
    filtered = request(f"store/catalog/products?brandId={brand['data']['id']}&sort=priceAsc")
    assert filtered["code"] == 200 and filtered["data"]["total"] == 1, filtered
    # Category tree cycle rejection and nested product visibility.
    child = request("admin/product/category", {"categoryName": "验收子分类", "categoryCode": code + "-C", "parentId": category["id"], "status": 1, "sortOrder": 0}, token=token)
    assert child["code"] == 200, child
    assert request("admin/product/category", {"id": category["id"], "categoryName": "日用好物", "categoryCode": "VERIFY", "parentId": child["data"]["id"], "status": 1, "sortOrder": 0}, method="PUT", token=token)["code"] == 400
    product_edit["categoryId"] = child["data"]["id"]
    assert request("admin/product", product_edit, method="PUT", token=token)["code"] == 200
    nested = request(f"store/catalog/products?categoryId={category['id']}&brandId={brand['data']['id']}")
    assert nested["data"]["total"] == 1
    product_edit["status"] = 0
    assert request("admin/product", product_edit, method="PUT", token=token)["code"] == 200
    assert request(f"store/catalog/products/{pid}")["code"] == 404
    product_edit["status"] = 1
    assert request("admin/product", product_edit, method="PUT", token=token)["code"] == 200
    # Stock history records initial balance and all committed changes.
    history = request(f"admin/product/{pid}/skus/{sid}/history?pageSize=100", token=token)
    assert history["data"]["total"] == 12, history
    assert history["data"]["records"][-1]["actor"].startswith("erp:")
    # Multipart upload checks actual bytes, persistence, replacing and clearing images.
    def upload(contents, filename):
        boundary = "test" + uuid.uuid4().hex
        payload = (f"--{boundary}\r\nContent-Disposition: form-data; name=\"file\"; filename=\"{filename}\"\r\nContent-Type: image/png\r\n\r\n").encode() + contents + f"\r\n--{boundary}--\r\n".encode()
        req = urllib.request.Request("http://localhost:18080/api/admin/upload/image", data=payload,
                                     headers={"Authorization": "Bearer " + token, "Content-Type": "multipart/form-data; boundary=" + boundary})
        with urllib.request.urlopen(req, timeout=15) as response:
            return json.load(response)
    assert upload(b"not an image", "fake.png")["code"] == 400
    pixel = base64.b64decode("iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aWuoAAAAASUVORK5CYII=")
    uploaded = upload(pixel, "valid.png")
    assert uploaded["code"] == 200, uploaded
    url = uploaded["data"]
    # Main image must enforce the same scheme and ownership rules as the gallery.
    for bad_url, expected in [("javascript:alert(1)", 400), ("/api/media/not-owned.png", 403)]:
        product_edit["mainImage"] = bad_url
        assert request("admin/product", product_edit, method="PUT", token=token)["code"] == expected
    product_edit["mainImage"] = url
    assert request("admin/product", product_edit, method="PUT", token=token)["code"] == 200
    for _ in range(2):
        assert request(f"admin/product/{pid}/images", [url], token=token)["code"] == 200
    assert request(f"admin/product/{pid}/images", token=token)["data"] == [url]
    with urllib.request.urlopen("http://localhost:18080" + url, timeout=10) as response:
        assert response.status == 200
    assert request(f"admin/product/{pid}/images", [], token=token)["code"] == 200
    assert request(f"admin/product/{pid}/images", token=token)["data"] == []
    assert request(f"admin/product/{pid}/images", ["javascript:alert(1)"], token=token)["code"] == 400
    # All enabled SKU variants are needed for public visibility; price range validation remains strict.
    existing_skus = request(f"admin/product/{pid}/skus", token=token)["data"]
    for variant in existing_skus:
        variant["status"] = 0
        assert request(f"admin/product/{pid}/skus", variant, method="PUT", token=token)["code"] == 200
    assert request(f"store/catalog/products/{pid}")["code"] == 404
    for variant in existing_skus:
        variant["status"] = 1
        assert request(f"admin/product/{pid}/skus", variant, method="PUT", token=token)["code"] == 200
    # Deleting a product must make inventory mutation fail without changing stock or its journal.
    retired = request("admin/product", {"productCode": code + "-RETIRED", "productName": "删除库存校验", "categoryId": category["id"], "salePrice": 1, "stock": 7}, token=token)["data"]
    retired_sku = request(f"admin/product/{retired['id']}/skus", token=token)["data"][0]
    assert request(f"admin/product/{retired['id']}", method="DELETE", token=token)["code"] == 200
    assert request(f"admin/product/{retired['id']}/skus/{retired_sku['id']}/stock", {"delta": 1, "reason": "禁止修改删除商品"}, token=token)["code"] == 404
    with db.cursor() as cursor:
        cursor.execute("SELECT stock FROM product_sku WHERE id=%s", (retired_sku["id"],))
        assert cursor.fetchone()[0] == 7
        cursor.execute("SELECT COUNT(*) FROM inventory_log WHERE sku_id=%s", (retired_sku["id"],))
        assert cursor.fetchone()[0] == 1
    for port in [18081, 18082]:
        for path in ["/doc.html", "/v3/api-docs"]:
            with urllib.request.urlopen(f"http://localhost:{port}{path}", timeout=10) as response:
                assert response.status == 200
    Path(".runtime/catalog-result.json").write_text(json.dumps({"productId": pid, "skuId": sid, "checks": "passed", "concurrentWrites": 15, "successfulDeductions": 10, "brandAndCategory": "passed", "mediaReplaceAndClear": "passed", "stockHistory": "passed", "knife4j": "passed"}, ensure_ascii=False), encoding="utf-8")
    print("PASS: authentication, registration isolation, category/product/SKU CRUD, catalogue DTO, filters and 15 concurrent inventory changes")
    db.close()


if __name__ == "__main__":
    main()
