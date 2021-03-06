package hust.software.garbage.service.impl;

import hust.software.garbage.common.CommonResult;
import hust.software.garbage.common.UploadUtil;
import hust.software.garbage.dto.SingleGarbage;
import hust.software.garbage.mgb.mapper.GarbageMapper;
import hust.software.garbage.mgb.mapper.GarbageTypeMapper;
import hust.software.garbage.mgb.mapper.SearchAccountMapper;
import hust.software.garbage.mgb.model.*;
import hust.software.garbage.service.GarbageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @author: 小栗旬
 * @Date: 2020/1/2 9:42
 */
@Service
public class GarbageServiceImpl implements GarbageService {
    @Autowired
    private GarbageMapper garbageMapper;
    @Autowired
    private GarbageTypeMapper garbageTypeMapper;
    @Autowired
    private SearchAccountMapper searchAccountMapper;

    @Autowired
    private UploadUtil uploadUtil;

    @Override
    public CommonResult listGarbagesByName(String name) {
        List<SingleGarbage> garbages = garbageMapper.listGarbageByName(name);
        if (garbages.isEmpty()) {
            return CommonResult.success("查找不到");
        }
        return CommonResult.success(garbages);
    }

    @Override
    public CommonResult listGarbagesByType(Integer typeId) {

        GarbageType garbageType = garbageTypeMapper.selectByPrimaryKey(typeId);
        if (garbageType == null) {
            return CommonResult.failed("错误的垃圾类别");
        }
        GarbageExample example = new GarbageExample();
        GarbageExample.Criteria criteria = example.createCriteria();
        criteria.andTypeIdEqualTo(typeId);

        List<Garbage> garbages = garbageMapper.selectByExample(example);
        return CommonResult.success(garbages);
    }

    @Override
    public CommonResult listGarbagesByTypeWithPage(Integer typeId, Integer page, Integer size) {
        Integer start = (page - 1) * size;
        GarbageType garbageType = garbageTypeMapper.selectByPrimaryKey(typeId);
        if (garbageType == null) {
            return CommonResult.failed("错误的垃圾类别");
        }
        return CommonResult.success(garbageMapper.listGarbagesByTypeWithPage(typeId, start, size));
    }

    @Override
    public CommonResult listRandomGarbages() {
        return CommonResult.success(garbageMapper.listRandomGarbage());
    }

    @Override
    public CommonResult addGarbage(String name, Integer typeId) {
        GarbageExample garbageExample = new GarbageExample();
        GarbageExample.Criteria criteria = garbageExample.createCriteria();
        criteria.andNameEqualTo(name);
        boolean exist = garbageMapper.countByExample(garbageExample) >= 1;
        if (exist) {
            return CommonResult.success("该垃圾已存在");
        }

        Garbage garbage = new Garbage();
        garbage.setName(name);
        garbage.setTypeId(typeId);
        int count = garbageMapper.insert(garbage);
        if (count >= 1) {
            return CommonResult.success("添加成功", garbage);
        }
        return CommonResult.failed("添加失败");
    }

    @Override
    public CommonResult deleteGarbage(Integer id) {
        boolean influenced = garbageMapper.deleteByPrimaryKey(id) >= 1;
        if (influenced) {
            return CommonResult.success("删除成功");
        }
        return CommonResult.failed("删除失败");
    }


    @Override
    public CommonResult modifyGarbage(Integer id, String name, Integer typeId) {
        GarbageExample garbageExample = new GarbageExample();
        GarbageExample.Criteria criteria = garbageExample.createCriteria();
        criteria.andIdNotEqualTo(id);
        criteria.andNameEqualTo(name);
        boolean exist = garbageMapper.countByExample(garbageExample) >= 1;
        if (exist) {
            return CommonResult.success("该垃圾名称已存在");
        }

        Garbage garbage = new Garbage();
        garbage.setId(id);
        garbage.setName(name);
        garbage.setTypeId(typeId);
        boolean influenced = garbageMapper.updateByPrimaryKey(garbage) >= 1;
        if (influenced) {
            return CommonResult.success("修改成功");
        } else {
            return CommonResult.failed("修改失败");
        }
    }

    @Override
    public CommonResult identifyGarbage(MultipartFile file) {
        if (file.isEmpty()) {
            return CommonResult.failed("上传图片为空");
        }
        String fileName = file.getOriginalFilename();
        String suffixName = fileName.substring(fileName.lastIndexOf("."));
        if (!".jpg".equals(suffixName) && !".png".equals(suffixName)) {
            return CommonResult.failed("请上传jpg或png格式图片");
        }
        String identifyResult = uploadUtil.uploadPicAndIdentify(file);

        String typeName =identifyResult.split("/")[0];
        String garbageName = identifyResult.split("/")[1];
        addCount(typeName);
        SingleGarbage singleGarbage = new SingleGarbage();
        singleGarbage.setTypeId(garbageTypeMapper.getIdByTypeName(typeName));
        singleGarbage.setName(garbageName);
        return CommonResult.success("识别成功", singleGarbage);
    }

    /**
     * 给类别查询次数+1
     * @param typeName 垃圾类别
     */
    private void addCount(String typeName) {
        searchAccountMapper.addAccount(typeName);
    }

    @Override
    public CommonResult getAccount(){
        SearchAccountExample example = new SearchAccountExample();
        List<SearchAccount> searchAccounts = searchAccountMapper.selectByExample(example);
        return CommonResult.success(searchAccounts);
    }


}
