@PostMapping("/submit")
public Mono<ResponseEntity<String>> submit(@RequestBody String payload) {
    return Mono.just(payload)
        .flatMap(p -> repository.save(new MyData(p)))  // Reactive DB
        .flatMap(data -> kafkaSender.send(Mono.just(SenderRecord.create(new ProducerRecord<>("topic", data.getPayload()), null))))
        .then(Mono.just(ResponseEntity.ok("Accepted"))); // returns quickly
}
