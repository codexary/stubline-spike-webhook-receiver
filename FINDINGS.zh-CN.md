[English](FINDINGS.md) | 简体中文

# 实验结论

## 观察

接收端按时间戳和原始正文验证 HMAC-SHA256，拒绝超过五分钟窗口的请求，并记住事件 ID。演示中，合法通知和重复通知都返回 204；伪造或过期通知返回 401。ID 只保存在内存里，重启后会丢失。

## 对后端的影响

签名通知仍可能重复送达；正式接入回调时还需验证来源、限制重放，并持久去重。这不能使扣款请求本身幂等。后端以 `Idempotency-Key` 处理扣款请求，目前还没有接入支付方 webhook。参见[后端 ADR 0008：Keyed payment invocations](https://github.com/codexary/stubline-backend/blob/main/docs/adr/0008-keyed-payment-invocations.md)。
