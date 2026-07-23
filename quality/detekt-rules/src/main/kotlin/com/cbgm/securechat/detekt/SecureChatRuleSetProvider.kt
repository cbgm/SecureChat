package com.cbgm.securechat.detekt

import com.cbgm.securechat.detekt.rule.DaoUsageRule
import com.cbgm.securechat.detekt.rule.LayerDependencyRule
import com.cbgm.securechat.detekt.rule.NoNotNullAssertionRule
import com.cbgm.securechat.detekt.rule.NoPlatformImportInCommonMainRule
import com.cbgm.securechat.detekt.rule.NoTestImportInProductionRule
import com.cbgm.securechat.detekt.rule.RepositoryDependencyRule
import com.cbgm.securechat.detekt.rule.UseCaseDependencyRule
import com.cbgm.securechat.detekt.rule.ViewModelDirectDataDependencyRule
import com.cbgm.securechat.detekt.rule.WeakHashAlgorithmRule
import dev.detekt.api.RuleName
import dev.detekt.api.RuleSet
import dev.detekt.api.RuleSetId
import dev.detekt.api.RuleSetProvider

class SecureChatRuleSetProvider : RuleSetProvider {

    override val ruleSetId =
        RuleSetId("SecureChat")

    override fun instance(): RuleSet {
        return RuleSet(
            id = ruleSetId,
            rules = mapOf(
                RuleName("NoPlatformImportInCommonMainRule") to
                    ::NoPlatformImportInCommonMainRule,

                RuleName("NoTestImportInProductionRule") to
                    ::NoTestImportInProductionRule,

                RuleName("NoNotNullAssertionRule") to
                    ::NoNotNullAssertionRule,

                RuleName("LayerDependencyRule") to
                    ::LayerDependencyRule,

                RuleName("ViewModelDirectDataDependencyRule") to
                    ::ViewModelDirectDataDependencyRule,

                RuleName("UseCaseDependencyRule") to
                    ::UseCaseDependencyRule,

                RuleName("RepositoryDependencyRule") to
                    ::RepositoryDependencyRule,

                RuleName("DaoUsageRule") to
                    ::DaoUsageRule,

                RuleName("WeakHashAlgorithmRule") to
                    ::WeakHashAlgorithmRule,
            ),
        )
    }
}
