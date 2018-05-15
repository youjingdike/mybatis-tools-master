# mybatis-tools
生成数据库对应的MyBatis配置文件的工具应用，目前支持MySql和Postgresql数据库。

支持生成数据库表对应的java domain类和MyBatis的映射文件, domain类名从表名转化而来，类名和表名的映射关系：
1. 表名中如果有"_"，则把"_"后面的首字母大写。 如 user_order 表对应的domain类名为 UserOrder.java，对应的映射文件名称也是UserOrder.xml。

