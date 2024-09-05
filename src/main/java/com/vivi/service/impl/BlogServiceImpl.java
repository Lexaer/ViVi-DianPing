package com.vivi.service.impl;

import com.vivi.entity.Blog;
import com.vivi.mapper.BlogMapper;
import com.vivi.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;


@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

}
