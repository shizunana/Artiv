package com.artiv.guide.client;

import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/** 可替换传输用于无费用测试；真实请求禁止重定向，并限制完整响应的大小和等待时间。 */
@FunctionalInterface
interface GuideHttpTransport {
    Response send(HttpRequest request, int timeoutSeconds) throws Exception;
    record Response(int status, byte[] body) { }

    static GuideHttpTransport jdk() {
        var client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NEVER).build();
        return (request, seconds) -> {
            var pending = client.sendAsync(request, ignored -> new LimitedBody());
            try {
                var result = pending.get(seconds, TimeUnit.SECONDS);
                return new Response(result.statusCode(), result.body());
            } catch (TimeoutException | InterruptedException e) {
                pending.cancel(true);
                throw e;
            }
        };
    }

    final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final HttpResponse.BodySubscriber<byte[]> delegate = HttpResponse.BodySubscribers.ofByteArray();
        private Flow.Subscription subscription;
        private int received;
        private boolean stopped;
        @Override public CompletionStage<byte[]> getBody() { return delegate.getBody(); }
        @Override public void onSubscribe(Flow.Subscription subscription) {
            this.subscription = subscription;
            delegate.onSubscribe(subscription);
        }
        @Override public void onNext(List<ByteBuffer> buffers) {
            if (stopped) return;
            for (var buffer : buffers) {
                if (buffer.remaining() > 262144 - received) {
                    stopped = true;
                    subscription.cancel();
                    delegate.onError(new GuideModelException("模型响应超过大小限制"));
                    return;
                }
                received += buffer.remaining();
            }
            delegate.onNext(buffers);
        }
        @Override public void onError(Throwable error) { if (!stopped) delegate.onError(error); }
        @Override public void onComplete() { if (!stopped) delegate.onComplete(); }
    }
}
