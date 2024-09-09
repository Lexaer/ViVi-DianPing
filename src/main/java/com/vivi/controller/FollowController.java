package com.vivi.controller;


import com.vivi.dto.Result;
import com.vivi.service.IFollowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/follow")
public class FollowController {
    @Autowired
    private IFollowService followService;

    @PutMapping("/{followUserId}/{isFollow}")
    public Result follow(@PathVariable("followUserId") Long followUserId ,@PathVariable("isFollow") boolean isFollow){
        return followService.follow(followUserId,isFollow);
    }

    @GetMapping("/or/not/{followUserId}")
    public Result follow(@PathVariable("followUserId") Long followUserId ){
        return followService.isFollow(followUserId);
    }
    @GetMapping("/common/{id}")
    public Result commonFollows(@PathVariable("id") Long id){
        //此 id 为你想看的对方的userId
        return followService.commonFollows(id);
    }

}
