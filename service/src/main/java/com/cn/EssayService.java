package com.cn;

import com.cn.config.RabbitConfig;
import com.cn.dao.ClassifyRepository;
import com.cn.dao.CommentRepository;
import com.cn.dao.CustomizeRepository;
import com.cn.dao.EssayRepository;
import com.cn.entity.*;
import com.cn.enums.CommonState;
import com.cn.util.MailUtil;
import com.cn.util.RestUtil;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.ModelMap;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author ngcly
 */
@Service
public class EssayService {
    @Autowired
    private EssayRepository essayRepository;
    @Autowired
    private ClassifyRepository classifyRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private CustomizeRepository customizeRepository;
    @Autowired
    private BookService bookService;
    @Autowired
    private MailUtil mailUtil;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * 获取文章列表
     * @return
     */
    public ModelMap getEssayList(int page,int pageSize){
        String sql = "SELECT t.id,t.title,t.synopsis,t3.username,t2.name,t.created_time,t.updated_time,t.read_num " +
                "FROM essay t,classify t2,user t3 WHERE t.classify_id=t2.id AND t.user_id=t3.id " +
                "AND (t.state=3 OR t.state=4) order by t.updated_time desc LIMIT ?,?";
        List<Map<String,Object>> essays = customizeRepository.nativeQueryListMap(sql,(page-1)*pageSize,pageSize);
        return RestUtil.success(essays);
    }

    /**
     * 获取用户的文章列表
     * @param pageable 分页
     * @param userId 用户ID
     * @return
     */
    public ModelMap getUserEssayList(Pageable pageable,String userId){
        Page<Essay> essayList = essayRepository.findAll(pageable);
        return RestUtil.success(essayList.getContent());
    }

    /**
     * 根据ID获取文章详情
     * @param id 文章ID
     * @return
     */
    public Essay getEssayDetail(String id){
        return essayRepository.getById(id);
    }

    /**
     * 文章阅读数+1
     */
    @Transactional(rollbackFor = Exception.class)
    public void readEssay(String id){
        essayRepository.readOne(id);
    }

    /**
     * 用户写文章
     * @param essay 文章内容
     */
    @Transactional(rollbackFor = Exception.class)
    public ModelMap createEssay(Long classifyId, Essay essay){
        Classify classify = classifyRepository.getOne(classifyId);
        essay.setClassify(classify);
        essay.setReadNum(0);
        essay.setState(CommonState.EssayState.DRAFT.getCode());
        return RestUtil.success(essayRepository.save(essay).getId());
    }

    /**
     * 用户修改文章
     * @param essay 文章内容
     */
    @Transactional(rollbackFor = Exception.class)
    public ModelMap updateEssay(Long classifyId, Essay essay){
        essay.setState(CommonState.EssayState.PENDING.getCode());
        Classify classify = classifyRepository.getOne(classifyId);
        essay.setClassify(classify);
        essay.setReadNum(0);
        essayRepository.save(essay);
        return RestUtil.success();
    }

    /**
     * 用户删除文章
     * @param userId  用户ID
     * @param essayId 文章ID
     */
    @Transactional(rollbackFor = Exception.class)
    public ModelMap delUserEssay(String userId,String essayId){
        int num = essayRepository.deleteEssayByIdAndUserId(essayId,userId);
        return num>0?RestUtil.success():RestUtil.failure(500,"删除失败");
    }

    /**
     * 根据条件获取文章列表
     * @param pageable 分页
     * @param essay  条件
     */
    public Page<Essay> getEssayList(Pageable pageable, Essay essay){
        return essayRepository.findAll(EssayRepository.getEssayList(essay),pageable);
    }

    /**
     * 审查文章
     * @param essay
     */
    @Transactional(rollbackFor = Exception.class)
    public void altEssayState(Essay essay){
        Essay essay1 = essayRepository.getOne(essay.getId());
        essay1.setState(essay.getState());
        //审核不通过
        if(essay.getState()==2){
            essay1.setRemark(essay.getRemark());
            News news = new News();
            news.setUserId(essay1.getUser().getId());
            news.setContent(essay.getRemark());
            news.setCreateTime(new Date());
            news.setSenderId("1");
            news.setSendTime(new Date());
            //在线消息通知 由于后台服务与api服务分开部署 无法在此直接发送websocket消息 所以通过Rabbit转发
            rabbitTemplate.convertAndSend(RabbitConfig.NOTIFY_QUEUE,news);
            //邮件通知作者
            mailUtil.sendSimpleMail(essay1.getUser().getEmail(),"文章审核不通过",essay1.getTitle()+"审核失败，理由： "+essay.getRemark());
        }else{
            Book book = new Book();
            book.setId(essay1.getId());
            book.setAuthor(essay1.getUser().getUsername());
            book.setTitle(essay1.getTitle());
            book.setContent(essay1.getContent());
            bookService.save(book);
        }
    }

    /**
     * 获取文章评论
     * @param id     文章Id
     * @param page   页数
     */
    public Page<Map<String,Object>> getComments(String id,int page){
        return commentRepository.selectComments(id,PageRequest.of(page-1,20));
    }

    /**
     * 评论文章
     * @param userId 用户Id
     * @param comment 评论内容
     */
    public ModelMap addComments(String userId,Comment comment){
        comment.setUserId(userId);
        commentRepository.save(comment);
        return RestUtil.success();
    }

    /**
     * 获取用户 点赞/收藏 文章
     * @param userId 用户Id
     * @param faveType 类型
     * @return Page<Essay>
     */
    public ModelMap getUserFavesEssay(String userId,Byte faveType,Pageable pageable){
        Page<Essay> essays = essayRepository.findUserFaveEssay(userId,faveType,pageable);
        return RestUtil.success(essays);
    }
}
