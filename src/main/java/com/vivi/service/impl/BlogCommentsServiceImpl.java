package com.vivi.service.impl;

import com.vivi.entity.BlogComments;
import com.vivi.mapper.BlogCommentsMapper;
import com.vivi.service.IBlogCommentsService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;


@Service
public class BlogCommentsServiceImpl extends ServiceImpl<BlogCommentsMapper, BlogComments> implements IBlogCommentsService {

}
