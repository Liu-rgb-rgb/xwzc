# r = float(input("请输入圆的半径："))
# PI = 3.14
# d = 2 * r
# area = PI * r * r
# print("圆的直径：", d)
# print("圆的面积：", area)

# total = 29.5
# used = 4 * 3
# remain = total - used
# times = remain / 2.5
# if remain % 2.5 != 0:
#     times = int(times) + 1
# else:
#     times = int(times)
# print("还需要运送{}次".format(times))

# i = 0
# while i < 100:
#     if i % 2 == 0:
#         print(i, end=" ")
#     i += 1


# num = float(input("请输入一个数字："))
# if num > 0:
#     print("这是正数")
# elif num < 0:
#     print("这是负数")
# else:
#     print("这个数是0，既不是正数也不是负数")


for n in range(2,100):
    flag = True 
    for i in range(2,n):
        if n % i == 0:
            flag = False
            break
    if flag:
        print(n, end=" ")