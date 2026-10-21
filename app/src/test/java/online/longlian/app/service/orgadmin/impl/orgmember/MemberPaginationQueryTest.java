package online.longlian.app.service.orgadmin.impl.orgmember;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.mapper.UserMapper;
import online.longlian.app.pojo.bo.orgadmin.OrgMemberListParamsBO;
import online.longlian.app.pojo.entity.OrganizationMember;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;

import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MemberPaginationQueryTest {

    @BeforeAll
    static void registerEntity() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""),
                OrganizationMember.class);
    }

    @ParameterizedTest
    @NullSource
    @EnumSource(SortDirection.class)
    void shouldKeepMembersInStableOrderAcrossPages(SortDirection direction) throws Exception {
        var query = new MemberQueryBuilder(mock(UserMapper.class)).buildMemberListQuery(
                OrgMemberListParamsBO.builder().orgId(1L).orderDir(direction).build());
        // 在内存库执行实际查询条件，验证分页结果而不是 SQL 字符串的写法。
        String segment = query.getCustomSqlSegment().replaceAll("#\\{[^}]+}", "?");
        try (var connection = DriverManager.getConnection("jdbc:sqlite::memory:");
             var statement = connection.createStatement()) {
            statement.execute("CREATE TABLE organization_member (id BIGINT PRIMARY KEY, org_id BIGINT, joined_at TEXT)");
            try (var insert = connection.prepareStatement("INSERT INTO organization_member VALUES (?, ?, ?)")) {
                for (long id = 1; id <= 44; id++) {
                    insert.setLong(1, id);
                    insert.setLong(2, id == 44 ? 2L : 1L);
                    insert.setString(3, id == 1 ? "2026-01-02 12:00:00" : "2026-01-01 12:00:00");
                    insert.executeUpdate();
                }
            }
            boolean ascending = direction == SortDirection.ASC;
            List<Long> expected = new ArrayList<>(LongStream.rangeClosed(2, 43)
                    .map(id -> ascending ? id : 45 - id).boxed().toList());
            expected.add(ascending ? expected.size() : 0, 1L);
            List<Long> actual = new ArrayList<>();
            try (var page = connection.prepareStatement(
                    "SELECT id FROM organization_member " + segment + " LIMIT 10 OFFSET ?")) {
                page.setObject(1, query.getParamNameValuePairs().values().iterator().next());
                for (int offset = 0; offset < 60; offset += 10) {
                    page.setInt(2, offset);
                    List<Long> ids = new ArrayList<>();
                    try (var rows = page.executeQuery()) {
                        while (rows.next()) {
                            ids.add(rows.getLong(1));
                        }
                    }
                    int start = Math.min(offset, 43);
                    assertThat(ids).containsExactlyElementsOf(expected.subList(start, Math.min(start + 10, 43)));
                    actual.addAll(ids);
                }
            }
            assertThat(actual).containsExactlyElementsOf(expected).doesNotHaveDuplicates();
        }
    }
}
