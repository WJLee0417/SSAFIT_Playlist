package com.ssafy.ssafit.runningrecord.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ssafy.ssafit.mapper.DatabaseIntegrationTestSupport;
import com.ssafy.ssafit.member.domain.Member;
import com.ssafy.ssafit.runningrecord.domain.RunningRecord;

class RunningRecordMapperIntegrationTest
        extends DatabaseIntegrationTestSupport {

    @Autowired
    private RunningRecordMapper runningRecordMapper;

    @Test
    void runningRecordMapperSupportsCrudAndSoftDelete() {
        Member member = insertMember("running_record_member");
        RunningRecord record = new RunningRecord();
        record.setMemberId(member.getId());
        record.setRecordDate(java.time.LocalDate.of(2026, 6, 20));
        record.setDistanceKm(new java.math.BigDecimal("5.00"));
        record.setDurationSeconds(1800);
        record.setPaceSecondsPerKm(360);
        record.setAverageCadence(172);
        record.setAverageHeartRateBpm(154);
        record.setMemo("Mapper running record");

        runningRecordMapper.insert(record);

        RunningRecord olderRecord = new RunningRecord();
        olderRecord.setMemberId(member.getId());
        olderRecord.setRecordDate(java.time.LocalDate.of(2026, 6, 19));
        olderRecord.setDistanceKm(new java.math.BigDecimal("3.00"));
        olderRecord.setDurationSeconds(1200);
        olderRecord.setPaceSecondsPerKm(400);
        olderRecord.setMemo("Older mapper running record");
        runningRecordMapper.insert(olderRecord);
        Member otherMember = insertMember("running_record_other_member");

        assertThat(record.getId()).isNotNull();
        RunningRecord selected = runningRecordMapper.selectById(record.getId())
                .orElseThrow();
        assertThat(selected.getMemberId()).isEqualTo(member.getId());
        assertThat(selected.getDistanceKm())
                .isEqualByComparingTo(new java.math.BigDecimal("5.00"));
        assertThat(selected.getAverageCadence()).isEqualTo(172);
        assertThat(selected.getAverageHeartRateBpm()).isEqualTo(154);
        assertThat(selected.getCreatedAt()).isNotNull();
        assertThat(selected.getUpdatedAt()).isNotNull();
        assertThat(runningRecordMapper.selectListByMemberId(member.getId()))
                .extracting(RunningRecord::getId)
                .containsExactly(record.getId(), olderRecord.getId());
        assertThat(runningRecordMapper.selectListByMemberId(otherMember.getId()))
                .isEmpty();

        record.setDistanceKm(new java.math.BigDecimal("10.00"));
        record.setDurationSeconds(3300);
        record.setPaceSecondsPerKm(330);
        record.setAverageCadence(176);
        record.setAverageHeartRateBpm(158);
        runningRecordMapper.update(record);

        RunningRecord updatedRecord = runningRecordMapper
                .selectById(record.getId())
                .orElseThrow();
        assertThat(updatedRecord.getPaceSecondsPerKm()).isEqualTo(330);
        assertThat(updatedRecord.getAverageCadence()).isEqualTo(176);
        assertThat(updatedRecord.getAverageHeartRateBpm()).isEqualTo(158);
        assertThat(runningRecordMapper.selectById(olderRecord.getId())
                .orElseThrow()
                .getAverageCadence()).isNull();
        assertThat(runningRecordMapper.selectById(olderRecord.getId())
                .orElseThrow()
                .getAverageHeartRateBpm()).isNull();

        runningRecordMapper.delete(record.getId());

        assertThat(runningRecordMapper.selectById(record.getId())).isEmpty();
        assertThat(runningRecordMapper.selectListByMemberId(member.getId()))
                .extracting(RunningRecord::getId)
                .containsExactly(olderRecord.getId());
        assertThat(countRows("running_records")).isEqualTo(2);
    }
}
