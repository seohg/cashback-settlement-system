package com.larva.cashback.domain.member;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
public class MemberRepositoryTest {

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("Member 저장 후 ID로 정상 조회 확인")
    void findById() {
        //given
        Member member = Member.builder()
                .email("larva@larva.com")
                .name("larva")
                .build();
        //when
        Member savedMember = memberRepository.save(member);
        Member foundMember = memberRepository.findById(savedMember.getId()).orElseThrow();

        //then
        assertThat(foundMember.getEmail()).isEqualTo(savedMember.getEmail());
        assertThat(foundMember.getName()).isEqualTo(savedMember.getName());
        assertThat(foundMember.getCreatedAt()).isNotNull();
    }
    @Test
    @DisplayName("Member 저장 후 email로 정상 조회 확인")
    void findByEmail() {
        //given
        Member member = Member.builder()
                .email("larva@larva.com")
                .name("larva")
                .build();
        //when
        Member savedMember = memberRepository.save(member);
        Member foundMember = memberRepository.findByEmail(savedMember.getEmail()).orElseThrow();

        //then
        assertThat(foundMember.getEmail()).isEqualTo(savedMember.getEmail());
        assertThat(foundMember.getName()).isEqualTo(savedMember.getName());
        assertThat(foundMember.getCreatedAt()).isNotNull();
    }
    @Test
    @DisplayName("optional 정상 리턴확인")
    void emailNotFound() {

        //when
        Optional<Member> member = memberRepository.findByEmail("test@larva.com");

        //then
        assertThat(member).isEmpty();
    }

}
