package com.swordmaster.player.service;

import com.swordmaster.common.BusinessException;
import com.swordmaster.currency.CurrencyType;
import com.swordmaster.currency.service.CurrencyService;
import com.swordmaster.equipment.sword.table.SwordTable;
import com.swordmaster.player.dto.PlayerResponse;
import com.swordmaster.player.entity.Player;
import com.swordmaster.player.entity.PlayerArtifact;
import com.swordmaster.player.repository.PlayerArtifactRepository;
import com.swordmaster.player.repository.PlayerRepository;
import com.swordmaster.user.entity.User;
import com.swordmaster.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlayerService {
    private final PlayerRepository         playerRepository;
    private final PlayerArtifactRepository playerArtifactRepository;
    private final UserRepository           userRepository;
    private final CurrencyService          currencyService;
    private final SwordTable               swordTable;

    // 조회 (모든 정보)
    public PlayerResponse getMe(Long userId) {
        Player player = playerRepository.findByUser_Id(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "플레이어 정보가 없습니다."));

        List<PlayerArtifact>    artifacts  = playerArtifactRepository.findAllByPlayer_User_Id(userId);
        Map<CurrencyType, Long> currencies = currencyService.getAll(player);

        return PlayerResponse.of(player, currencies, artifacts);
    }

    // 생성
    @Transactional
    public PlayerResponse create(Long userId) {
        // 비정상 접근에 대한 예외 처리
        if (playerRepository.existsByUser_Id(userId)) {
            throw new BusinessException(HttpStatus.CONFLICT, "플레이어가 이미 존재합니다.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "유저 정보가 없습니다."));

        Player player;

        // 같은 요청이 동시에 들어올 경우에 대한 Unique 처리
        try {
            // Flush를 통해 트랜잭션이 끝날 때 까지 미루지 않고 바로 INSERT 요청
            player = playerRepository.saveAndFlush(new Player(user, swordTable.getFirst().level()));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(HttpStatus.CONFLICT, "플레이어가 이미 존재합니다.");
        }

        // 같은 트랜잭션 내에서 재화 생성
        Map<CurrencyType, Long> currencies = currencyService.createAll(player);

        // 생성 시 인벤토리는 빈 상태로 생성되므로 응답 객체에 빈 리스트로 초기화
        return PlayerResponse.of(player, currencies, List.of());
    }
}
