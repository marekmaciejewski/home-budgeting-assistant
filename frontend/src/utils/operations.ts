import type { OperationResponse } from "../apiTypes";

export function toSortedOperations(operations: OperationResponse[]): OperationResponse[] {
  return [...operations].sort((left, right) => {
    const timeDifference = new Date(right.timestamp).getTime() - new Date(left.timestamp).getTime();
    return timeDifference === 0 ? right.id - left.id : timeDifference;
  });
}

export function describeOperation(operation: OperationResponse): string {
  if (!operation.sourceRegisterId && operation.targetRegisterId) {
    return `Recharge into ${operation.targetRegisterId}`;
  }

  if (operation.sourceRegisterId && operation.targetRegisterId) {
    return `${operation.sourceRegisterId} to ${operation.targetRegisterId}`;
  }

  return "Balance operation";
}

export function operationKind(operation: OperationResponse): "Recharge" | "Transfer" {
  return operation.sourceRegisterId ? "Transfer" : "Recharge";
}
