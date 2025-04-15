package com.example.aideep.tts;


import java.util.ArrayList;
import java.util.List;

public class TTSParams {

    public String vcn = "xiaoyan";
    public int pitch = 50;
    public int speed = 50;
    public int volume = 50;

    /**
     * 测试文本资源，一下资源顺序与发音人、语种相对应
     *
     * @return
     */


    public static List<ParamInfo> getVCN() {
        List<ParamInfo> vcnList = new ArrayList<>();
        vcnList.add(new ParamInfo("xiaoyan", "讯飞小燕"));
        vcnList.add(new ParamInfo("aisjiuxu", "讯飞许久"));
        vcnList.add(new ParamInfo("aisxping", "讯飞小萍"));
        vcnList.add(new ParamInfo("aisjinger", "讯飞小婧"));
        vcnList.add(new ParamInfo("aisbabyxu", "讯飞许小宝"));
        return vcnList;
    }

}
