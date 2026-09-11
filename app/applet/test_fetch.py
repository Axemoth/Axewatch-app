import urllib.request
import re
import json

headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/138.0.0.0 Safari/537.36"}

def test_url(url):
    try:
        req = urllib.request.Request(url, headers=headers)
        res = urllib.request.urlopen(req, timeout=10)
        return res.read().decode("utf-8", "ignore")
    except Exception as e:
        return f"ERROR: {e}"

html = test_url("https://www.investorgain.com/report/ipo-gmp-live/331/")
print("Investorgain html length:", len(html))
chunks = re.findall(r'/_next/static/chunks/[a-zA-Z0-9_\-\.]+\.js', html)
print("Chunks:", len(set(chunks)))
for c in set(chunks):
    js = test_url("https://www.investorgain.com" + c)
    if "api" in js or "reportTableData" in js or "fetch" in js:
        apis = re.findall(r'["\'](/api/[^"\']+)["\']', js)
        if apis:
            print("APIs in", c, apis)
