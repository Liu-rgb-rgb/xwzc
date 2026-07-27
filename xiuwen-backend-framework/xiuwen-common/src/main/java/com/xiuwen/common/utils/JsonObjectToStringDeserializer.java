package com.xiuwen.common.utils;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;

/**
 * 自定义反序列化器：将 JSON 对象自动转换为 JSON 字符串
 */
public class JsonObjectToStringDeserializer extends JsonDeserializer<String> {

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // 获取当前节点的树模型
        ObjectMapper mapper = (ObjectMapper) p.getCodec();
        JsonNode node = mapper.readTree(p);
        
        // 如果节点为空，返回 null
        if (node == null || node.isNull()) {
            return null;
        }
        
        // 关键步骤：将节点重新写回为字符串
        // 这样无论是传入的对象 {"x":1} 还是字符串 "abc"，都会统一变成 String 存入变量
        return node.toString();
    }
}