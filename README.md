# \# 绣纹智创 - 核心代码说明

# 

# \## 🚀 快速了解本项目（强烈推荐）

# 由于项目包含完整的企业级分布式架构（Lombok + Redis + MySQL），直接运行需配置复杂的环境。

# 建议评委优先观看演示视频（网盘链接），视频包含了完整的系统闭环演示。

# 

# \## 🔍 核心代码导读

# 如果你想查看我们的核心实现，请参阅以下文件：

# 1\. \*\*防超卖与并发控制\*\*：`xiuwen-backend-framework/src/main/java/.../OrderService.java`（Redis分布式锁 + 数据库乐观锁）。

# 2\. \*\*图像矢量化与DST生成\*\*：`py/api.py`（FastAPI接口）及 `py/step1\~5\_\*.py`（OpenCV轮廓提取 + pyembroidery写入DST）。

# 3\. \*\*数据库表结构\*\*：`xiuwen\_zhichuang\_v1\_schema.sql`（包含用户表、商品表、订单表及设计记录表）。

