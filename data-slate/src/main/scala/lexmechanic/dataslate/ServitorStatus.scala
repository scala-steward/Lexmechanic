package lexmechanic.dataslate

enum ServitorStatus(val label: String):
  case Offline extends ServitorStatus("Daemon offline")
  case Busy extends ServitorStatus("Cogitating…")
  case Failed extends ServitorStatus("Error")
  case Ready extends ServitorStatus("Ready")
