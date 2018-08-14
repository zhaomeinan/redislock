package com.example.redislock.demo;

import javax.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * @Author: zhaomeinan
 * @Description: 基于redis实现的分布式锁
 * @Date: Create in 11:12 2018/8/13
 * @Modificd By:
 */
@Component
public class RedisDistributedLock {

  @Resource
  private RedisTemplate redisTemplate;

  //private static final int DEFAULT_ACQUIRY_RESOLUTION_MILLIS = 100;

  /**
   * 锁超时时间，防止线程在入锁以后，无限的执行等待
   */
  private int expireMsecs = 60 * 1000;

  /**
   * 锁等待时间，防止线程饥饿
   */
  private int timeoutMsecs = 10 * 1000;

  /**
   * @Author: zhaomeinan
   * @Description: 获取锁的值
   * @Date: 17:31 2018/8/13
   * @Modificd By:
   * @Param: [key]
   * @return: java.lang.String
   * @throw: 请描述异常信息
   */
  private String get(final String key) {
    Object obj = redisTemplate.opsForValue().get(key);
    return obj != null ? obj.toString() : null;
  }

  /**
   * @Author: zhaomeinan
   * @Description: 设置锁的值
   * @Date: 17:32 2018/8/13
   * @Modificd By:
   * @Param: [key, value]
   * @return: boolean
   * @throw: 请描述异常信息
   */
  private boolean setNX(final String key, final String value) {
    Object obj = redisTemplate.opsForValue().setIfAbsent(key, value);
    return obj != null ? (Boolean) obj : false;
  }

  /**
   * @Author: zhaomeinan
   * @Description: 获取锁原来的值并设置新的值
   * @Date: 17:33 2018/8/13
   * @Modificd By:
   * @Param: [key, value]
   * @return: java.lang.String
   * @throw: 请描述异常信息
   */
  private String getSet(final String key, final String value) {
    Object obj = redisTemplate.opsForValue().getAndSet(key, value);
    return obj != null ? (String) obj : null;
  }

  /**
   * @Author: zhaomeinan
   * @Description: 获得 lock. 实现思路: 主要是使用了redis 的setnx命令,缓存了锁. reids缓存的key是锁的key,所有的共享,
   * value是锁的到期时间(注意:这里把过期时间放在value了,没有时间上设置其超时时间) 执行过程: 1.通过setnx尝试设置某个key的值,成功(当前没有这个锁)则返回,成功获得锁
   * 2.锁已经存在则获取锁的到期时间,和当前时间比较,超时的话,则设置新的值
   * @Date: 16:45 2018/8/13
   * @Modificd By:
   * @Param: [lockKey]
   * @return: boolean
   * @throw: 请描述异常信息
   */
  public boolean lock(String lockKeyIn) throws InterruptedException {
    String lockKey = this.getLockKey(lockKeyIn);

    int timeout = timeoutMsecs;

    while (timeout >= 0) {
      long expires = System.currentTimeMillis() + expireMsecs + 1;

      String expiresStr = String.valueOf(expires); //锁到期时间

      if (this.setNX(lockKey, expiresStr)) {
        return true;
      }

      String currentValueStr = this.get(lockKey); //redis里的时间

      //判断是否为空，不为空的情况下，如果被其他线程设置了值，则第二个条件判断是过不去的
      if (currentValueStr != null && Long.parseLong(currentValueStr) < System.currentTimeMillis()) {
        //获取上一个锁到期时间，并设置现在的锁到期时间，
        //只有一个线程才能获取上一个线上的设置时间，因为jedis.getSet是同步的
        String oldValueStr = this.getSet(lockKey, expiresStr);

        //防止误删（覆盖，因为key是相同的）了他人的锁——这里达不到效果，这里值会被覆盖，但是因为什么相差了很少的时间，所以可以接受
        //[分布式的情况下]:如过这个时候，多个线程恰好都到了这里，但是只有一个线程的设置值和当前值相同，他才有权利获取锁
        if (oldValueStr != null && oldValueStr.equals(currentValueStr)) {
          return true;
        }
      }

      //获取随机等待毫秒数
      int acquiryResolutionMillis = this.getAcquiryResolutionMillis();
      timeout -= acquiryResolutionMillis;
      //随机等待acquiryResolutionMillis时间后再次获取锁
      Thread.sleep(acquiryResolutionMillis);
    }

    return false;
  }

  /**
   * @Author: zhaomeinan
   * @Description: 释放锁
   * @Date: 11:58 2018/8/13
   * @Modificd By:
   * @Param:
   * @return:
   * @throw: 请描述异常信息
   */
  public void unlock(String lockKeyIn) {
    String lockKey = this.getLockKey(lockKeyIn);
    String currentValueStr = this.get(lockKey); //redis里的时间
    //锁未超时，删除锁
    if (currentValueStr != null && Long.parseLong(currentValueStr) >= System.currentTimeMillis()) {
      redisTemplate.delete(lockKey);
    }
  }

  /**
   * @Author: zhaomeinan
   * @Description: 获取key
   * @Date: 16:33 2018/8/13
   * @Modificd By:
   * @Param: [lockKey]
   * @return: java.lang.String
   * @throw: 请描述异常信息
   */
  public String getLockKey(String lockKey) {
    return lockKey + "_lock";
  }

  /**
   * @Author: zhaomeinan
   * @Description: 随机产生一个80-100的数字，用于获取锁的等待时间
   * @Date: 16:34 2018/8/13
   * @Modificd By:
   * @Param: []
   * @return: int
   * @throw: 请描述异常信息
   */
  public int getAcquiryResolutionMillis() {
    return (int) (Math.random() * (100 - 80 + 1) + 80);
  }

  /**
   * @Author: zhaomeinan
   * @Description: 测试锁的获取、释放demo
   * @Date: 16:41 2018/8/13
   * @Modificd By:
   * @Param:
   * @return:
   * @throw: 请描述异常信息
   */
  @Async
  public void seckill(int i) throws InterruptedException {
    String key = "orderno";
    // 返回锁的value值，供释放锁时候进行判断
    boolean getLock = this.lock(key);
    if (getLock) {
      System.out.println(Thread.currentThread().getName() + "获得了锁" + i);
      //Thread.sleep(10);
      System.out.println(i);
      this.unlock(key);
    } else {
      System.out.println(Thread.currentThread().getName() + "未获得锁" + i);
    }
  }

}
