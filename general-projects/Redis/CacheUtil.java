package com.ford.decisionplatform.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Component
public class CacheUtil {

    public static final String SYMBOL_UNDERSCORE = "_";
    public static final String JSON_FILE_EXT = ".json"; // Consider if this belongs here if blob download is removed

    
    private final RedisTemplate<String, String> redisTemplate;

    public CacheUtil(RedisTemplate<String, String> redisTemplate){
        this.redisTemplate = redisTemplate;
    }

    
    public void setValue(String key, String value) {
        if (key == null || key.isBlank()) { // Added key check
            log.warn("CacheUtil: Attempted to set with null or blank key.");
            return;
        }
        if (value != null && !value.isBlank()) {
            redisTemplate.opsForValue().set(key, value);
            log.info("CacheUtil: Loaded response in cache (no expiry) with key {}", key);
        } else {
            log.warn("CacheUtil: Attempted to set null or blank value for key {}", key);
        }
    }

    
    public void setValue(String key, String value, long timeout, TimeUnit unit) {
        if (key == null || key.isBlank()) { // Added key check
            log.warn("CacheUtil: Attempted to set with null or blank key.");
            return;
        }
        if (value != null && !value.isBlank()) {
            redisTemplate.opsForValue().set(key, value, timeout, unit);
            log.info("CacheUtil: Loaded response in cache with key {} and expiry {} {}", key, timeout, unit);
        } else {
            log.warn("CacheUtil: Attempted to set null or blank value with expiry for key {}", key);
        }
    }

    
    public String getValue(String key) {
        if (key == null || key.isBlank()) { // Added key check
            log.warn("CacheUtil: Attempted to get with null or blank key.");
            return null; // Or throw IllegalArgumentException
        }
        return redisTemplate.opsForValue().get(key);
    }


    public Map<String,String> getValues(List<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            log.warn("CacheUtil: Attempted to get with null or blank key.");
            return null;
        }
        List<String> values = Optional
                .ofNullable(redisTemplate.opsForValue().multiGet(keys))
                .orElse(Collections.nCopies(keys.size(), null));

        return IntStream.range(0, keys.size())
                .boxed()
                .collect(
                        LinkedHashMap::new,
                        (m, i) -> m.put(keys.get(i), values.get(i)),
                        Map::putAll
                );
    }


    public Boolean remove(String key) {
        if (key == null || key.isBlank()) { // Added key check
            log.warn("CacheUtil: Attempted to remove with null or blank key.");
            return null; // Or throw IllegalArgumentException
        }
        return redisTemplate.delete(key);
    }

    
    public Boolean hasKey(String key) {
        if (key == null || key.isBlank()) { // Added key check
            log.warn("CacheUtil: Attempted to check for null or blank key.");
            return false; // Or throw IllegalArgumentException
        }
        return redisTemplate.hasKey(key);
    }

    // This method should ideally be removed from CacheUtil
    // The responsibility of downloading content should be outside this class.
    // If you must keep it, consider injecting Storage and StorageUtil if they are Spring beans.
    /*
    public void setValue(String REQUEST_ID, String targetApi, Storage storage, String bucketName) {
        String content = StorageUtil.downloadContentFromBlob(REQUEST_ID, targetApi, storage, bucketName);
        setValue(REQUEST_ID + SYMBOL_UNDERSCORE + targetApi, content);
        log.info("CacheUtil: Loaded cache with key {} from blob {}/{}", REQUEST_ID + SYMBOL_UNDERSCORE + targetApi,
                REQUEST_ID, REQUEST_ID + SYMBOL_UNDERSCORE + targetApi + JSON_FILE_EXT);
    }
    */
}
