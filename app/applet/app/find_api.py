import urllib.request
import re

headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"}
req = urllib.request.Request("https://www.investorgain.com/report/ipo-gmp-live/331/", headers=headers)
html = urllib.request.urlopen(req).read().decode("utf-8", "ignore")

chunks = re.findall(r'/_next/static/chunks/[^"\'`\s]+\.js', html)
for c in set(chunks):
    try:
        js = urllib.request.urlopen(urllib.request.Request("https://www.investorgain.com" + c, headers=headers), timeout=5).read().decode("utf-8", "ignore")
        if "cloud/v2" in js:
            print("Found in chunk:", c)
            for m in re.findall(r'baseURL:[^,{}]+|https?://[^"\'`\s]+', js):
                print("   candidate:", m)
    except Exception as e:
        pass
