/** Public surface of the i18n subsystem (FE-ADR-012). */
export { I18nService } from './i18n.service';
export { TranslatePipe } from './translate.pipe';
export {
  DEFAULT_LANGUAGE,
  LANGUAGES,
  LANGUAGE_STORAGE_KEY,
  isLanguage,
  type Language,
} from '../../shared/i18n-catalog';
export { CATALOG, type TranslationKey } from '../../shared/i18n-catalog';
