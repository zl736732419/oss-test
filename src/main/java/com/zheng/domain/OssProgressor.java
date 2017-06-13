package com.zheng.domain;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * oss文件上传下载进度
 * Created by zhenglian on 2017/6/12.
 */
public class OssProgressor {
    /**
     * 当前进度,默认为0
     */
    private double cur = 0d;
    /**
     * 总进度,默认为100
     */
    private double total = 100d;
    /**
     * 是否完成
     */
    private boolean finished = false;
    /**
     * 表示是否有错
     */
    private boolean error = false;
    /**
     * 错误信息
     */
    private String errorMessage;

    public OssProgressor() {
    }
    
    public OssProgressor(double cur, double total) {
        this.cur = cur;
        this.total = total;
    }

    /**
     * 进行常规的容错处理
     */
    private void checkValid() {
        this.cur = (this.cur < 0) ? 0 : this.cur;
        this.total = (this.total < 0) ? 0 : this.total;
        this.cur = (this.cur > this.total) ? this.total : this.cur;
    }

    public boolean isError() {
        return error;
    }

    public double getCur() {
        return cur;
    }

    public void setCur(double cur) {
        //容错处理
        this.cur = (cur > 0) ? cur : 0;
        this.cur = (this.cur > this.total) ? this.total : this.cur;
        this.finished = (this.cur == this.total);
    }

    public double getTotal() {
        return total;
    }

    public void setTotal(double total) {
        //容错处理
        this.total = (total < 0 || total < this.cur) ? this.cur : total;
    }

    /**
     * 获取百分比数值表示形式
     * @return
     */
    public double getPercent() {
        checkValid();
        BigDecimal curVal = new BigDecimal(this.cur);
        BigDecimal totalVal = new BigDecimal(this.total);
        BigDecimal percent = curVal.divide(totalVal, 4, RoundingMode.HALF_UP).multiply(new BigDecimal(100d));
        
        return percent.doubleValue();
    }

    /**
     * 获取百分比显示字符串
     * @return
     */
    public String getPercentLabel() {
        double percent = getPercent();
        return percent + "%";
    }

    public boolean isFinished() {
        return finished;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * 上传失败，设置错误信息
     * @param errorMessage
     */
    public void setErrorMessage(String errorMessage) {
        this.finished = true;
        this.error = true;
        this.errorMessage = errorMessage;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("finished", this.finished)
                .append("hasError", this.error)
                .append("cur", this.cur)
                .append("total", this.total)
                .build();
    }
}
