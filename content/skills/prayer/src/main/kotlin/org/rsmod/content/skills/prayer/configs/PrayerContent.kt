package org.rsmod.content.skills.prayer.configs

import org.rsmod.api.type.refs.content.ContentReferences

/**
 * Content groups this module owns.
 *
 * These are resolved from `.data/symbols/.local/content.sym` rather than upstream's `content.sym`.
 * `ContentReferenceResolver` fails a boot outright on an unknown name (`ImplicitNameNotFound`), so
 * the symbol file and this object have to stay in step.
 *
 * Note there is deliberately no separate group for "altars that accept bone offerings". An obj or
 * loc carries exactly one content group, and every offering altar is also a restore altar, so the
 * two would be mutually exclusive. Offering is driven by [PrayerParams.offer_xp_percent] instead.
 */
object PrayerContent : ContentReferences() {
    val prayer_bones = find("prayer_bones")
    val prayer_ashes = find("prayer_ashes")
    val prayer_altar = find("prayer_altar")
}
