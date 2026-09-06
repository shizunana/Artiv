package com.artiv.guide.tool;

/** 引用只能由后端已执行的白名单读取生成；不接受模型生成的 URL 或源码路径。 */
public record GuideReference(String kind, String id, String title, String detail, String checkedAt) { }
