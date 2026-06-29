[English](README.md) | 简体中文

# stubline-spike-webhook-receiver

这是一个 spike：研究我们该怎样接收支付方发来的 webhook。接收端要确认每条通知确实来自支付方，拒绝事后被重放的旧请求，并且即使支付方重发同一个事件，也只处理一次。它不用于生产环境。

判断规则：

- 支付方用共享密钥对 `timestamp.body`（ASCII 时间戳、一个点、原始请求正文的字节）计算 HMAC-SHA256 签名。签名不对返回 `401`。
- 时间戳与接收端时钟相差超过 5 分钟返回 `401`，这样截获的请求不能过后再拿来重放。
- 记住每个事件 JSON 里的 `id`。合法的重复事件返回 `204`，但不再处理，支付方收到后就不会继续重试。

## 构建与测试

需要 JDK 21。Maven wrapper（`./mvnw`）会自己下载项目指定版本的 Maven，不用另外安装。

```bash
./mvnw -q verify
```

## 运行

两个程序都从环境变量 `WEBHOOK_SECRET` 读取共享密钥；在两个终端里设置同一个演示值。

终端一，运行接收端：

```bash
export WEBHOOK_SECRET=demo-secret
./mvnw -q exec:java
```

它会输出 `Webhook receiver listening on 8081`。

终端二，运行发送端。它扮演支付方，依次发送一个正常事件、同一个事件的重复、一个伪造事件和一个过期事件：

```bash
export WEBHOOK_SECRET=demo-secret
./mvnw -q exec:java -Dexec.mainClass=com.elsworne.stubline.spike.webhook.WebhookSender
```

预期输出：

```text
Valid: 204
Duplicate: 204
Forged: 401
Stale: 401
```

记住的事件 `id` 只保存在内存里，重启就会丢失。上线版本需要把它们持久化，并决定保留多久。

接收端绑定本机回环地址，因为共享密钥与发送端仅供本地演示；只有本机能访问。

## 许可证

本仓库采用 [MIT License](LICENSE)。

## 关于本仓库

这个独立小项目记录 Stubline 的实验。结论写在 Stubline 主仓库的 ADR 中；上文记录了测试结果。

本仓库的代码与文档借助 AI 工具编写；记录的输出都来自实际运行。
