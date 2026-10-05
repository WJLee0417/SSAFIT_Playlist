package com.ssafy.ssafit.member.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ssafy.ssafit.mapper.DatabaseIntegrationTestSupport;
import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.member.domain.MemberStatus;
import com.ssafy.ssafit.runningprofile.domain.RunningProfile;
import com.ssafy.ssafit.runningprofile.mapper.RunningProfileMapper;

class MemberRunningProfileMapperIntegrationTest
        extends DatabaseIntegrationTestSupport {

    @Autowired
    private RunningProfileMapper runningProfileMapper;

    @Test
    void memberAndProfileMapperMapsGeneratedKeysAndEnums() {
        Member member = insertMember("mapper_member");
        RunningProfile profile = new RunningProfile();
        profile.setMemberId(member.getId());
        profile.setTargetBpmMin(130);
        profile.setTargetBpmMax(160);
        profile.setTargetCadenceMin(165);
        profile.setTargetCadenceMax(180);
        profile.setPreferredGenre("POP");

        runningProfileMapper.insert(profile);

        RunningProfile selected = runningProfileMapper
                .selectByMemberId(member.getId())
                .orElseThrow();
        assertThat(memberMapper.existsByLoginId("mapper_member")).isTrue();
        assertThat(memberMapper.selectById(member.getId()).orElseThrow().getStatus())
                .isEqualTo(MemberStatus.ACTIVE);
        assertThat(selected.getId()).isNotNull();
        assertThat(selected.getTargetBpmMin()).isEqualTo(130);
        assertThat(selected.getTargetCadenceMin()).isEqualTo(165);
        assertThat(selected.getTargetCadenceMax()).isEqualTo(180);

        profile.setTargetCadenceMin(170);
        profile.setTargetCadenceMax(184);
        assertThat(runningProfileMapper.update(profile)).isEqualTo(1);
        RunningProfile updated = runningProfileMapper
                .selectByMemberId(member.getId())
                .orElseThrow();
        assertThat(updated.getTargetCadenceMin()).isEqualTo(170);
        assertThat(updated.getTargetCadenceMax()).isEqualTo(184);
    }
}
