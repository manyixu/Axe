#mast分支暂停开发，请转移到branch-jdk1.7分支，内容最新。

####版本 0.1
####需要 jdk 版本 1.8 
----------

##支持特性（基本可以不用看，都是常用的东西）：
* IOC（[@Controller]()、[@Service]()、[@Autowired]()，使用[BeanHelper]()获取BeanMap）
* AOP/Proxy（[@Aspect]()注解切面类，实现[Proxy]()或者继承[AspectProxy]()加强切点）
* restful风格的Http请求支持，内建了POST、DELETE、PUT、GET类型，可以查看[RequestMethod]()枚举
* [@Tns]()数据库事务支持
* [FileParam]()文件上传（多文件、单文件）
* 主动规避重复action（如果定义了相同的action，框架初始化会报错）
* 路径参数pathParam，多拼参数，类似 [/get/{id}_{name}]() 这样
* actionMethod 方法参数的随意化，支持[HttpRequest]()、[HttpResponse]()、[Param]()、[Map<String,Object>]() 注入，注意Map<String,Object> 只会注入Body内容。
* [Filter]()过滤器 ，支持filter链（有序），支持在Controller上或Controller方法上[@FilterFuckOff]()排除指定Filter，默认排除所有
* dao层（miniDao4Mysql），底层是apache DbUtils，Bean映射框架自己实现（因为DbUtils不支持Date类型）
* dao层  支持表名用类名表示，表字段用类字段表示，类似HQL
* dao通过继承 [Repository]()接口可直接获得saveEntity、insertEntity、updateEntity、getEntity、deleteEntity直接面向对象的方法。
* dao内sql语句变量占位符，支持?，也支持?带数字，但是，不可以在一个语句内，同时出现两种模式
* dao的sql支持in，但是有要求，变量必须是List才好使，不可以是Array，还有，in后面要主动加(...)，另外提一点，有的框架in操作不支持List的size为0，此框架也不支持
* dao支持分页，使用[PageConfig]()，只要@Sql标注的方法，最后一个参数是pageConfig即可，@Sql内语句不用按分页效果写，原来查询怎么写还怎么写
* dao支持分页结果，使用[Page]()做接受参数，只要在@Sql标注的方法，返回类型是Page即可，几遍方法最后一个参数不是PageConfig，也会将数据包装成Page，此时Page.count是数据条数

----------

##待加入特性列表：
* 权限验证
* 文件下载
* TODO列表
* 多数据源
* dao层支持配置连接池
* dao层支持数据库主从分离
* 分布式服务管理支持

----------

##正在进行：
1.完善Filter
2.CreateTableUtil
3.框架管理界面
4.增加@RequestParam的default值
5.修改url path中非变量的部分，不支持"-"符号
6.增加Component注解
7.修改bug，@Autowired成员变量不支持继承

----------

##约定（这个要看，约定，是为了更方便）：
* Request.value 也就是url 只支持字母加数字，理论上也够用了，因此，url的 pathParam 变量名只支持 字母数字，这点框架启动会检测。
* post请求的payload 只支持json格式，不能是数组，key必须用英文双引号包围，value 必须区分数字字符串，如果是字符串纯字母，必须双引号包围。总之按规范来。
    * 这样是可以的：{"key":"value"} 或者 {"key":[{"subkey":"subvalue"}]} 等json格式
	* 这样不可以：[{"key":"value"}] 
	* 对JSON格式相关疑问可以参考[JSON 入门指南](http://www.ibm.com/developerworks/cn/web/wa-lo-json/)
* 下面的请求作为例子
	* 这样是可以的(查询参数可以是中文，随意)：
	```
	http://localhost:8080/getOne/[zhangjiagang]()/123_zhangsan/detail?p1=123&p2=张家港
	```
	* 这样是不可以的(404，路径请不要含有非字母、数字、下划线、$以外的字符)：
	``` 
	http://localhost:8080/getOne/[张家港]()/123_zhangsan/detail?p1=123&p2=张家港 
	```
第二种匹配是什么鬼!这是系统实际运行时决定的，这点框架启动时无法检测，只能不匹配路径返回404。
* Controller中的action处理方法，也就是加了@Request注解的方法，携带的参数，如果请求中能够取到，就自动赋值，如果请求中没有，默认为null。有些框架是会强制参数个数的，此框架不做参数个数强制。
因为默认是赋值null，所以参数不可以是基本类型的小写形式(语法糖)，因为这样是不能赋值null的，这点框架启动会检测。
* Controller中的action方法参数，除了基本类型的包装类，List中的泛型，数组类型，都不可以用自定义Bean，这部分框架尚未打算实现！为了约束，所以禁止使用自定义Bean类型，就是Pojo类型，这点框架启动会检测！
* Dao层不支持框架启动自动建表，考虑到支持多数据源，如果自动建表，Bean就没法复用来支持多数据源，根据dao来获取多数据源目录也变得非常复杂难搞，使用起来也会痛苦要死。
虽然不支持自动建表，框架也拱了小工具来手动运行建表，可以指定数据库地址。
* Dao 继承并使用 Repository接口时，注意接口内有戏方法是要求Entity类必须有@Id标注的字段的
* Dao 的自动解析机制，要求Entity类中的字段，必须有对应的set、get方法，方法名的后半部分采用驼峰标记，必须严格规范书写，否则解析器不会认作表字段处理
另外Entity如果有父类，不可以在子类中set、get父类的字段，解析器只会认识当前类中的字段与set、get方法，不会跨父子类匹配，这点需要注意，虽然少见。

----------
