package com.yjk.reggie.controller;

import com.yjk.reggie.common.R;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.util.UUID;

@RestController
@Slf4j
@RequestMapping("/common")
public class CommonController {

    @Value("${reggie.path}")
    private String basePath;

    /**
     * 文件上传
     * @param file
     * @return
     */
    @PostMapping("/upload")
    @SneakyThrows
    public R<String> upload(MultipartFile file){

        log.info(file.toString());

        //原始文件名
        String originalFilename = file.getOriginalFilename();
        //后缀名
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));

        //使用 UUID 重新生成文件名，防止文件名重复造成覆盖
        String fileName = UUID.randomUUID().toString() + suffix;

        File dir = new File(basePath);
        //判断是否存在该目录
        if(!dir.exists()){
            //目录不存在，需要创建
            dir.mkdirs();
        }

        //转存到指定目录
        file.transferTo(new File(basePath + fileName));

        return R.success(fileName);
    }

    /**
     * 文件下载
     * @param name
     * @param response
     */
    @GetMapping("/download")
    @SneakyThrows
    public void download(String name, HttpServletResponse response){

        //输入流，通过输入流读取文件内容
        FileInputStream fileInputStream = new FileInputStream(basePath + name);

        //输出流，通过输出流将文件写回浏览器，在浏览器展示图片
        ServletOutputStream outputStream = response.getOutputStream();

        response.setContentType("image/jpeg");

        int len = 0;
        byte[] bytes = new byte[1024];
        while((len = fileInputStream.read(bytes)) != -1){
            outputStream.write(bytes, 0, len);
            outputStream.flush();
        }

        outputStream.close();
        fileInputStream.close();
    }
}
