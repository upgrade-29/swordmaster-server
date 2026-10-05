package com.swordmaster.equipment.artifact;


import com.swordmaster.common.table.ConstantTable;
import com.swordmaster.common.table.GoogleSheetClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ArtifactConstantTable extends ConstantTable {
    public ArtifactConstantTable(
            GoogleSheetClient sheets,
            @Value("${game-data.gid.artifacts-constants}") long gid
    ) {
        super(sheets, gid);
    }
}
