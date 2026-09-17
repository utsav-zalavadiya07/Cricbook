/**
 * Formats a date string (e.g., YYYY-MM-DD or ISO string) to DD-MM-YYYY safely,
 * preventing timezone shift bugs.
 */
export function formatDate(dateString?: string): string {
  if (!dateString) return "";
  
  // If it's just a YYYY-MM-DD string, parse it cleanly to avoid timezone offsets
  if (dateString.length === 10 && dateString.includes("-")) {
    const parts = dateString.split("-");
    if (parts.length === 3) {
      return `${parts[2]}-${parts[1]}-${parts[0]}`;
    }
  }

  try {
    const date = new Date(dateString);
    if (isNaN(date.getTime())) return dateString;

    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const year = date.getFullYear();
    
    return `${day}-${month}-${year}`;
  } catch {
    return dateString;
  }
}

/**
 * Formats a date string to DD-MM-YYYY HH:mm:ss
 */
export function formatDateTime(dateString?: string): string {
  if (!dateString) return "";
  
  try {
    // If it's a date-only string but formatDateTime is called, handle it gracefully
    if (dateString.length === 10 && dateString.includes("-")) {
      const parts = dateString.split("-");
      if (parts.length === 3) {
        return `${parts[2]}-${parts[1]}-${parts[0]} 00:00:00`;
      }
    }

    const date = new Date(dateString);
    if (isNaN(date.getTime())) return dateString;

    const day = date.getDate().toString().padStart(2, '0');
    const month = (date.getMonth() + 1).toString().padStart(2, '0');
    const year = date.getFullYear();
    
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    const seconds = date.getSeconds().toString().padStart(2, '0');
    
    return `${day}-${month}-${year} ${hours}:${minutes}:${seconds}`;
  } catch {
    return dateString;
  }
}
