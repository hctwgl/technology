## base64的原理
    base64由64个字符组成：
        小写字母：a-z
        大小字母：A-Z
        数字：0-9
        符号：+ /
    
    为什么base64要用6个bit作为一组？
        base64一共选了64个字符来作为有效字符，2^6=64，因此需要6个bit才能把64个字符全部映射到。
        
    8bit的二进制数据，怎样进行base64的？
        比如，3个字符的二进制位数为：3 * 8bit = 24bit; 
            转化为base64，则为：4 * 6 = 24bit用base64表示，那么就会被映射为4个字符。
            --- 转化为base64字符串后，数据长度将比原来增加1/3的长度
    
    base64有什么应用价值？
        1、在简单应用场景，对于二进制文件，可以转化为base64字符串，直接使用http协议传输数据。比如，图片经常转化为base64字符串进行传输。
        2、实现信息弱隐藏的效果，比如一些参数base64后进行传输，从表面上起到隐藏原始数据的效果。
    
    base64的补位修正
        base64以6个bit为单元，每4个base64编码为一组。
        当原始数据的bit位不够4个base64编码时，修正方案为：
            比如，对字符"a"进行base64编码，a 的二进制 01100001
               a=011000010，分成Base64分组后为：011000 01，
               01不够6bit，需要补0为：010000，得到YQ，
               因为4个Base编码为一组，最后再补上'='补齐一组，即：YQ==
                
    
## base64字符串为什么一定要进行urlencode，才能在网络上传输？
    base64中包含+/两个符号，这两个符号在使用http协议进行网络传输时，有特殊含义，因此需要进行转义处理。
        
        +  在使用http协议进行参数传递时，+用来表示参数中的空格字符，urlencode之后，转化为%2B
        /  在使用http协议进行参数传递时，/用来表示路径的分隔符，比如http://www.abc.com/path/to/somewhere
        
    url保留字符 - Reserved characters after percent-encoding
    !	#	$	&	'	(	)	*	+	,	/	:	;	=	?	@	[	]
    %21	%23	%24	%26	%27	%28	%29	%2A	%2B	%2C	%2F	%3A	%3B	%3D	%3F	%40	%5B	%5D
    
    https://en.wikipedia.org/wiki/Percent-encoding
    
    

    
