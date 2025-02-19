package com.sky.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebSocketServer implements WebSocketHandler {

    // 存储每个会话的 Sinks.Many，用于发送消息
    private final Map<String, Sinks.Many<String>> sessionSinks = new ConcurrentHashMap<>();

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String sid = session.getHandshakeInfo().getUri().getPath().split("/")[2]; // 从 URI 中提取 sid
        Sinks.Many<String> sink = Sinks.many().multicast().onBackpressureBuffer();
        sessionSinks.put(sid, sink);

        // 处理接收到的消息
        Flux<WebSocketMessage> inbound = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                .doOnNext(message -> System.out.println("收到来自客户端" + sid + "的消息：" + message))
                .flatMap(message -> processMessage(sid, message))
                .map(session::textMessage);

        // 处理发送的消息
        Flux<WebSocketMessage> outbound = sink.asFlux()
                .map(session::textMessage);

        // 处理会话关闭
        Mono<Void> onClose = session.closeStatus()
                .doOnNext(status -> {
                    System.out.println("连接断开" + sid);
                    sessionSinks.remove(sid);
                })
                .then();

        return session.send(outbound)
                .and(session.receive().then())
                .then(onClose);
    }

    private Mono<String> processMessage(String sid, String message) {
        // 这里可以添加对收到消息的具体处理逻辑
        return Mono.just(message);
    }

    /**
     * 群发消息
     */
    public Mono<Void> sendAllClient(String message) {
        return Flux.fromIterable(sessionSinks.values())
                .flatMap(sink -> Mono.fromRunnable(() -> sink.tryEmitNext(message)))
                .then();
    }
}