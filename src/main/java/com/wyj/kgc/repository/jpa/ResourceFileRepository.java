package com.wyj.kgc.repository.jpa;

import com.wyj.kgc.entity.ResourceFile;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 资源文件的数据访问接口 (DAO)
 * JpaRepository 会自动为我们提供 增/删/改/查 的方法
 * <ResourceFile, Long> 意思是：
 * - 我们要操作的实体是 ResourceFile
 * - 这个实体的主键(id)类型是 Long
 */
public interface ResourceFileRepository extends JpaRepository<ResourceFile, Long> {
    // 暂时我们不需要任何自定义查询
    // JpaRepository 提供的 save(), findById() 等方法已经足够了
}