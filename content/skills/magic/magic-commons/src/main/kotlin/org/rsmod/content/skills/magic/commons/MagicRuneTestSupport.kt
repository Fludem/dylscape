package org.rsmod.content.skills.magic.commons

import com.google.inject.AbstractModule
import com.google.inject.Scopes
import kotlin.reflect.KClass
import org.rsmod.api.spells.runes.combo.ComboRuneRepository
import org.rsmod.api.spells.runes.combo.ComboRuneScript
import org.rsmod.api.spells.runes.compact.CompactRuneRepository
import org.rsmod.api.spells.runes.compact.CompactRuneScript
import org.rsmod.api.spells.runes.fake.FakeRuneRepository
import org.rsmod.api.spells.runes.fake.FakeRuneScript
import org.rsmod.api.spells.runes.staves.StaffSubstituteRepository
import org.rsmod.api.spells.runes.staves.StaffSubstituteScript
import org.rsmod.api.spells.runes.subs.RuneSubstituteRepository
import org.rsmod.api.spells.runes.subs.RuneSubstituteScript
import org.rsmod.api.spells.runes.unlimited.UnlimitedRuneRepository
import org.rsmod.api.spells.runes.unlimited.UnlimitedRuneScript
import org.rsmod.plugin.scripts.PluginScript

/**
 * What an integration test needs to make `MagicRuneManager` work outside the real server.
 *
 * On a normal boot upstream's `MagicRunesModule` binds the six rune repositories as singletons and
 * six scripts fill them from cache enums. The test harness installs no plugin modules, so a test
 * that only lists its own script gets empty, unbound repositories and every cast dies on a
 * `lateinit`. Pass [Module] as the child module of `runInjectedGameTest` and spread [scripts] into
 * its script list, ahead of the script under test.
 */
public object MagicRuneTestSupport {
    public val scripts: Array<KClass<out PluginScript>> =
        arrayOf(
            ComboRuneScript::class,
            CompactRuneScript::class,
            FakeRuneScript::class,
            StaffSubstituteScript::class,
            RuneSubstituteScript::class,
            UnlimitedRuneScript::class,
        )

    public class Module : AbstractModule() {
        override fun configure() {
            bind(ComboRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(CompactRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(FakeRuneRepository::class.java).`in`(Scopes.SINGLETON)
            bind(StaffSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(RuneSubstituteRepository::class.java).`in`(Scopes.SINGLETON)
            bind(UnlimitedRuneRepository::class.java).`in`(Scopes.SINGLETON)
        }
    }
}
