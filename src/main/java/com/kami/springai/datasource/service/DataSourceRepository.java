package com.kami.springai.datasource.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kami.springai.datasource.model.DataSourceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * 数据源配置持久化存储类
 */
@Component
public class DataSourceRepository {
    private static final String DATA_DIR = "data";
    private static final String DATA_SOURCE_FILE = "datasources.json";
    private final Path dataSourceFilePath;
    private final ObjectMapper objectMapper;
    private final PasswordEncryptor passwordEncryptor;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private List<DataSourceConfig> cachedConfigs = new ArrayList<>();
    
      @Autowired
    public DataSourceRepository(ObjectMapper objectMapper, PasswordEncryptor passwordEncryptor) {
        this.objectMapper = objectMapper;
        this.passwordEncryptor = passwordEncryptor;
        this.dataSourceFilePath = Paths.get(DATA_DIR, DATA_SOURCE_FILE);

        // 初始化数据目录
        try {
            Files.createDirectories(Paths.get(DATA_DIR));
            if (!Files.exists(dataSourceFilePath)) {
                // 创建空的数据源文件
                DataSourceConfigWrapper wrapper = new DataSourceConfigWrapper();
                wrapper.setDataSources(new ArrayList<>());
                saveToFile(wrapper);
            } else {
                // 加载现有配置
                loadFromFile();
            }
        } catch (IOException e) {
            throw new RuntimeException("初始化数据源存储失败", e);
        }
    }
    
    /**
     * 保存数据源配置
     */
    public void save(DataSourceConfig config) {
        lock.writeLock().lock();
        try {

            // 检查是否存在
            int index = -1;
            for (int i = 0; i < cachedConfigs.size(); i++) {
                if (cachedConfigs.get(i).getId().equals(config.getId())) {
                    index = i;
                    break;
                }
            }

            if (index >= 0) {
                // 更新现有配置
                cachedConfigs.set(index, config);
            } else {
                // 添加新配置
                cachedConfigs.add(config);
            }

            // 保存到文件
            try {
                DataSourceConfigWrapper wrapper = new DataSourceConfigWrapper();
                wrapper.setDataSources(cachedConfigs);
                saveToFile(wrapper);
            } catch (IOException e) {
                throw new RuntimeException("保存数据源配置失败: " + e.getMessage(), e);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 删除数据源配置
     */
    public void delete(String id) {
        lock.writeLock().lock();
        try {
            cachedConfigs = cachedConfigs.stream()
                .filter(config -> !config.getId().equals(id))
                .collect(Collectors.toList());
            
            // 保存到文件
            try {
                DataSourceConfigWrapper wrapper = new DataSourceConfigWrapper();
                wrapper.setDataSources(cachedConfigs);
                saveToFile(wrapper);
            } catch (IOException e) {
                throw new RuntimeException("保存数据源配置失败: " + e.getMessage(), e);
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    /**
     * 获取所有数据源配置
     */
    public List<DataSourceConfig> findAll() {
        lock.readLock().lock();
        try {
            // 返回解密后的配置副本
            return cachedConfigs.stream()
                .map(this::createDecryptedCopy)
                .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 根据ID获取数据源配置
     */
    public Optional<DataSourceConfig> findById(String id) {
        lock.readLock().lock();
        try {
            // 只从缓存中查找
            return cachedConfigs.stream()
                .filter(config -> config.getId().equals(id))
                .map(this::createDecryptedCopy)
                .findFirst();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 获取默认数据源
     */
    public Optional<DataSourceConfig> findDefault() {
        lock.readLock().lock();
        try {
            return cachedConfigs.stream()
                .filter(DataSourceConfig::isDefault)
                .findFirst()
                .map(this::createDecryptedCopy);
        } finally {
            lock.readLock().unlock();
        }
    }
    
    /**
     * 从文件加载配置
     */
    private void loadFromFile() throws IOException {
        DataSourceConfigWrapper wrapper = objectMapper.readValue(dataSourceFilePath.toFile(), DataSourceConfigWrapper.class);
        cachedConfigs = wrapper.getDataSources();
    }
    
    /**
     * 保存配置到文件
     */
    private void saveToFile(DataSourceConfigWrapper wrapper) throws IOException {
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(dataSourceFilePath.toFile(), wrapper);
    }
    
    /**
     * 创建用于保存的安全配置副本（不包含明文密码）
     */
    private DataSourceConfig createSecureCopy(DataSourceConfig original) {
        try {
            // 使用Jackson序列化然后反序列化，明文password字段会被@JsonIgnore忽略
            String json = objectMapper.writeValueAsString(original);
            return objectMapper.readValue(json, DataSourceConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("创建安全配置副本失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建解密后的配置副本
     */
    private DataSourceConfig createDecryptedCopy(DataSourceConfig original) {
        try {
            // 使用Jackson序列化然后反序列化
            String json = objectMapper.writeValueAsString(original);
            DataSourceConfig copy = objectMapper.readValue(json, DataSourceConfig.class);

            return copy;
        } catch (Exception e) {
            throw new RuntimeException("创建解密配置副本失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * JSON序列化包装类
     */
    private static class DataSourceConfigWrapper {
        private List<DataSourceConfig> dataSources;
        
        public List<DataSourceConfig> getDataSources() {
            return dataSources;
        }
        
        public void setDataSources(List<DataSourceConfig> dataSources) {
            this.dataSources = dataSources;
        }
    }
}