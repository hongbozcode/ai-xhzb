package com.xhzb.nursing.service.impl;

import java.io.InputStream;
import java.util.List;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.xhzb.common.utils.DateUtils;
import com.xhzb.oss.client.OSSAliyunFileStorageService;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.xhzb.nursing.mapper.KnowledgeBaseMapper;
import com.xhzb.nursing.domain.KnowledgeBase;
import com.xhzb.nursing.service.IKnowledgeBaseService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import java.util.Arrays;

/**
 * 知识库Service业务层处理
 * 
 * @author hongbozcode
 * @date 2026-11-03
 */
@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements IKnowledgeBaseService
{
    @Autowired
    private KnowledgeBaseMapper knowledgeBaseMapper;

    @Autowired
    private OSSAliyunFileStorageService ossAliyunFileStorageService;


    /**
     * 查询知识库
     * 
     * @param id 知识库主键
     * @return 知识库
     */
    @Override
    public KnowledgeBase selectKnowledgeBaseById(Long id)
    {
        return getById(id);
    }

    /**
     * 查询知识库列表
     * 
     * @param knowledgeBase 知识库
     * @return 知识库
     */
    @Override
    public List<KnowledgeBase> selectKnowledgeBaseList(KnowledgeBase knowledgeBase)
    {
        return knowledgeBaseMapper.selectKnowledgeBaseList(knowledgeBase);
    }

    /**
     * 新增知识库
     *
     * @param knowledgeBase 知识库
     * @return 结果
     */
    @Override
    public int insertKnowledgeBase(KnowledgeBase knowledgeBase)
    {
        //下载文件
        InputStream inputStream = ossAliyunFileStorageService.download(knowledgeBase.getDocumentUrl());
        //解析文件
        //调用agent端

        //收集所有文档ID
        List<String> docIdList = null; //python返回结果
        String docIdListJSON = JSONUtil.toJsonStr(docIdList);

        //保存数据库
        knowledgeBase.setCreateTime(DateUtils.getNowDate());
        knowledgeBase.setRemark(docIdListJSON);
        boolean flag = this.save(knowledgeBase);

        return flag ? 1 :0 ;

    }

    /**
     * 修改知识库
     * 
     * @param knowledgeBase 知识库
     * @return 结果
     */
    @Override
    public int updateKnowledgeBase(KnowledgeBase knowledgeBase)
    {
        return updateById(knowledgeBase)? 1 : 0;
    }

    /**
     * 批量删除知识库
     *
     * @param ids 需要删除的知识库主键
     * @return 结果
     */
    @Override
    public int deleteKnowledgeBaseByIds(Long[] ids)
    {
        // 查询所有知识库
        List<KnowledgeBase> knowledgeBaseList = this.listByIds(Arrays.asList(ids));
        if(CollUtil.isEmpty(knowledgeBaseList)) {
            return 0;
        }

        for (KnowledgeBase knowledgeBase : knowledgeBaseList) {
            // 删除OSS的数据
            String documentUrl = knowledgeBase.getDocumentUrl();
            ossAliyunFileStorageService.delete(documentUrl);

            // 删除向量库的数据
            String idsJSON = knowledgeBase.getRemark();
            List<String> idList = JSONUtil.toList(idsJSON, String.class);
            //调用python端agent

        }

        // 删除数据库的数据
        boolean result = this.removeByIds(Arrays.asList(ids));
        return result ? 1 :0;
    }

    /**
     * 删除知识库信息
     * 
     * @param id 知识库主键
     * @return 结果
     */
    @Override
    public int deleteKnowledgeBaseById(Long id)
    {
        return removeById(id)? 1 : 0;
    }
}
