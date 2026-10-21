import requests

# 1. 准备好你要传给服务器的图片（用你之前跑过的 pattern.png 就行）
# 注意：请确保当前文件夹里有一个叫 pattern.png 的图片
files = {'file': open('pattern.png', 'rb')}

# 2. 向你的本地服务器发请求
print("正在向服务器发图，计算针脚中...")
try:
    response = requests.post('http://127.0.0.1:8000/generate-dst', files=files)

    # 3. 打印服务器返回的结果
    if response.status_code == 200:
        print("🎉 接口调用成功！返回数据如下：")
        print(response.json())
    else:
        print(f"❌ 出错了，状态码：{response.status_code}")
        print(response.text)
except Exception as e:
    print(f"❌ 连接失败，请检查 api.py 是不是还在运行！报错：{e}")