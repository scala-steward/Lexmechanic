package lexmechanic.dataslate

import lexmechanic.dataslate.i18n.Messages

enum ServitorStatus(val label: Messages => String):
  case Offline extends ServitorStatus(_.statusOffline)
  case Busy extends ServitorStatus(_.statusBusy)
  case Failed extends ServitorStatus(_.statusFailed)
  case Ready extends ServitorStatus(_.statusReady)
