package com.larva.cashback.domain.batch;

import com.larva.cashback.domain.serviceapplication.ServiceApplication;
import com.larva.cashback.domain.serviceapplication.ServiceApplicationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CashbackItemWriter implements ItemWriter<List<ServiceApplication>> {

    private final ServiceApplicationRepository serviceApplicationRepository;

    @Override
    public void write(Chunk<? extends List<ServiceApplication>> chunk) throws Exception {
        int count = 0;
        List<ServiceApplication> all = new ArrayList<>();
        for (List<ServiceApplication> applications : chunk) {
            all.addAll(applications);
        }
        serviceApplicationRepository.saveAll(all);
    }
}