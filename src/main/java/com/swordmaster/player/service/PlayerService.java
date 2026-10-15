package com.swordmaster.player.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.equipment.sword.dto.SwordResponse;
import com.swordmaster.equipment.sword.table.SwordTable;
import com.swordmaster.player.dto.PlayerCreateRequest;
import com.swordmaster.player.entity.Player;
import com.swordmaster.player.repository.PlayerRepository;
import com.swordmaster.user.entity.User;
import com.swordmaster.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class PlayerService {
    private final PlayerRepository playerRepository;
    private final UserRepository   userRepository;      // 유저 조회용 (조인)
    private final SwordTable       swordTable;

    // 조회
    @Transactional(readOnly = true)
    public Player getMe(Long userId) {
        return playerRepository.findByUser_Id(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "플레이어 정보가 없습니다."));
    }

    @Transactional(readOnly = true)
    // 조회 - 닉네임 리스트 (랭킹 시스템 용)
    public Map<Long, String> getNicknames(Collection<Long> playerIds) {
        return playerRepository.findAllById(playerIds)
                .stream()
                .collect(Collectors.toMap(Player::getId, Player::getNickname));
    }

    // 생성
    public Player create(Long userId, PlayerCreateRequest request) {
        // 비정상 접근에 대한 예외 처리
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "유저 정보가 없습니다."));

        if (playerRepository.existsByUser_Id(userId)) throw new BusinessException(HttpStatus.CONFLICT, "플레이어가 이미 존재합니다.");

        // 닉네임 중복 처리
        String nickname = request.nickname();

        if (playerRepository.existsByNickname(nickname)) throw new BusinessException(HttpStatus.CONFLICT, "중복된 닉네임이 이미 존재합니다.");

        // 같은 요청이 동시에 들어올 경우에 대한 중복 요청 처리
        try {
            int    swordLevel = swordTable.getFirst().level();
            Player player     = new Player(user, nickname, swordLevel);

            return playerRepository.saveAndFlush(player);   // Flush를 통해 트랜잭션이 끝날 때 까지 미루지 않고 바로 INSERT 요청
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(HttpStatus.CONFLICT, "플레이어가 이미 존재하거나, 중복된 닉네임입니다.");
        }
    }

    // 변경
    public SwordResponse changeSwordLevel(Player player, int level) {
        player.changeSwordLevel(level);
        playerRepository.save(player);

        return SwordResponse.from(player);
    }

    // 클리어한 최고 스테이지 조회 (없으면 0)
    public int getClearedStage(Player player) {
        return player.getClearedStage();
    }

    // 클리어한 최고 스테이지 갱신 (기존 기록보다 높을 때만), 갱신 후 최고 스테이지 반환
    // 컬럼만 UPDATE하므로 인자로 받은 player에는 반영되지 않음
    public int updateClearedStage(Player player, int stage) {
        playerRepository.updateClearedStage(player.getId(), stage);

        return Math.max(player.getClearedStage(), stage);
    }
}
