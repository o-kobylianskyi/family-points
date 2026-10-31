package com.olehkobylianskyi.familypoints.service;

import com.olehkobylianskyi.familypoints.entity.UserAccount;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MembershipEpochBoundaryTest {
    private final WorkspaceEpochTransactionGuard guard=mock(WorkspaceEpochTransactionGuard.class);

    private <T> T service(Class<T> type, List<Object> dependencies) throws Exception {
        var constructor=type.getConstructors()[0];
        var args=new ArrayList<Object>();
        for (var parameter:constructor.getParameterTypes()) {
            Object arg=parameter==WorkspaceEpochTransactionGuard.class ? guard
                    : parameter==String.class ? "fixture" : parameter==boolean.class ? true : mock(parameter);
            args.add(arg);
            if (arg!=guard && parameter!=String.class && parameter!=boolean.class) dependencies.add(arg);
        }
        return type.cast(constructor.newInstance(args.toArray()));
    }

    @Test void allManagerWritesRejectStaleEpochBeforeRepositoriesMailOrDomainLookups() throws Exception {
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT,"stale")).when(guard).requireCurrentRequestEpoch(7L);
        record Call(Class<?> type,String method,Class<?>[] types,Object[] args) {}
        var calls=List.of(
            new Call(WorkspaceInvitationService.class,"issue",new Class[]{Long.class,Long.class},new Object[]{7L,3L}),
            new Call(WorkspaceInvitationService.class,"revoke",new Class[]{Long.class,Long.class},new Object[]{7L,3L}),
            new Call(WorkspaceJoinService.class,"rotateJoinCode",new Class[]{Long.class},new Object[]{7L}),
            new Call(WorkspaceJoinService.class,"disableJoinCode",new Class[]{Long.class},new Object[]{7L}),
            new Call(WorkspaceJoinService.class,"approveRequest",new Class[]{Long.class,Long.class,Long.class,String.class},new Object[]{7L,3L,4L,"Member"}),
            new Call(WorkspaceJoinService.class,"rejectRequest",new Class[]{Long.class,Long.class},new Object[]{7L,3L}),
            new Call(MemberEmailInvitationService.class,"issue",new Class[]{Long.class,Long.class,String.class},new Object[]{7L,3L,"fixture@example.org"}),
            new Call(UserAccountService.class,"createAccount",new Class[]{Long.class,Long.class,String.class,String.class},new Object[]{7L,3L,"fixture","password"}),
            new Call(UserAccountService.class,"updateAccount",new Class[]{Long.class,Long.class,String.class,boolean.class},new Object[]{7L,3L,"fixture",false}),
            new Call(UserAccountService.class,"resetPassword",new Class[]{Long.class,Long.class,String.class},new Object[]{7L,3L,"password"})
        );
        for (var call:calls) {
            var dependencies=new ArrayList<Object>();
            Object service=service(call.type(),dependencies);
            var failure=assertThrows(InvocationTargetException.class,()->call.type().getMethod(call.method(),call.types()).invoke(service,call.args()));
            assertEquals(409,assertInstanceOf(ResponseStatusException.class,failure.getCause()).getStatusCode().value(),call.method());
            verifyNoInteractions(dependencies.toArray());
            assertNotNull(call.type().getMethod(call.method(),call.types()).getAnnotation(org.springframework.transaction.annotation.Transactional.class));
        }
        verify(guard,times(calls.size())).requireCurrentRequestEpoch(7L);
    }

    @Test void membershipLinkRejectsBeforeMemberLookupAndSave() throws Exception {
        var dependencies=new ArrayList<Object>();
        var service=service(AccountMembershipLinkService.class,dependencies);
        var account=mock(UserAccount.class);
        when(account.getId()).thenReturn(5L); when(account.isEnabled()).thenReturn(true);
        doThrow(new ResponseStatusException(HttpStatus.CONFLICT,"stale")).when(guard).requireCurrentRequestOrLockedEpoch(7L);
        assertEquals(409,assertThrows(ResponseStatusException.class,()->service.linkExistingAccount(account,7L,3L)).getStatusCode().value());
        verifyNoInteractions(dependencies.toArray());
    }
}
