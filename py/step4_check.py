import pyembroidery
pattern = pyembroidery.read_dst('output.dst')
print(f"DST文件读取成功，总针数：{len(pattern.stitches)}")