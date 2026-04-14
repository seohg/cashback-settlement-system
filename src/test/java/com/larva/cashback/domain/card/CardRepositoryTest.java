package com.larva.cashback.domain.card;

import com.larva.cashback.domain.member.Member;
import com.larva.cashback.domain.member.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@DataJpaTest
public class CardRepositoryTest {
    @Autowired
    private CardRepository cardRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Test
    @DisplayName("Card 저장 후 ID로 조회")
    void saveCard() {
        // given
        Member member = Member.builder()
                .email("larvq@larva.com")
                .name("김라바")
                .build();
        memberRepository.save(member);

        Card card = Card.builder()
                .member(member)
                .cardProductCode("A300012")
                .cardNumber("1234-5678-9101-1121")
                .cardLimit(100000000)
                .build();

        // when
        Card saved = cardRepository.save(card);
        Card found = cardRepository.findById(saved.getId()).orElseThrow();

        // then
        assertThat(found.getCardNumber()).isEqualTo("1234-5678-9101-1121");
        assertThat(found.getMember().getId()).isEqualTo(member.getId()); // FK 확인
        assertThat(found.getStatus()).isEqualTo(CardStatus.ACTIVE);
        assertThat(found.getUsedAmount()).isEqualTo(0);
    }
}
