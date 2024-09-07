package com.vivi.service;

public interface ILock {

    boolean tryLock(Long timeoutSec);

    void unLock();
}
